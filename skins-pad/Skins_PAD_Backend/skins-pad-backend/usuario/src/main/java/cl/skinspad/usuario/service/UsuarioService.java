package cl.skinspad.usuario.service;

import cl.skinspad.usuario.domain.Rol;
import cl.skinspad.usuario.domain.Sesion;
import cl.skinspad.usuario.domain.Usuario;
import cl.skinspad.usuario.dto.EstadoUsuarioRequest;
import cl.skinspad.usuario.dto.LoginRequest;
import cl.skinspad.usuario.dto.LoginResponse;
import cl.skinspad.usuario.dto.MovimientoSaldoRequest;
import cl.skinspad.usuario.dto.RegistroRequest;
import cl.skinspad.usuario.repository.SesionRepository;
import cl.skinspad.usuario.repository.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UsuarioService {

  private final cl.skinspad.usuario.repository.MovimientoRepository movimientos;
  private final UsuarioRepository usuarios;
  private final SesionRepository sesiones;
  private final CorreoService correo;
  private final PasswordEncoder encoder;

  @Transactional
  public Usuario registrar(RegistroRequest request) {
    String username = request.username().trim();
    String email = request.email().trim().toLowerCase();

    if (usuarios.existsByUsernameIgnoreCase(username)) {
      throw new IllegalArgumentException("Ese nombre de usuario ya está en uso");
    }
    if (usuarios.existsByEmailIgnoreCase(email)) {
      throw new IllegalArgumentException("Ya existe una cuenta con ese correo");
    }
    if (request.fechaNacimiento().plusYears(18).isAfter(LocalDate.now())) {
      throw new IllegalArgumentException("Debes tener al menos 18 años");
    }

    Usuario usuario = new Usuario();
    usuario.setId("u-" + UUID.randomUUID());
    usuario.setNombre(request.nombre().trim());
    usuario.setUsername(username);
    usuario.setEmail(email);
    usuario.setPasswordHash(encoder.encode(request.password()));
    usuario.setFechaNacimiento(request.fechaNacimiento());
    usuario.setLinkSteam(request.linkSteam());
    usuario.setRol(Rol.cliente);
    usuario.setSuspendido(false);
    usuario.setVerificado(false);
    usuario.setSaldo(BigDecimal.ZERO);
    correo.encolar(
        "REGISTRO-" + usuario.getId(),
        email,
        "Registro en Skins PAD",
        "Hola " + usuario.getNombre() + ", tu cuenta " + username + " quedó registrada.");
    return usuarios.save(usuario);
  }

  @Transactional
  public LoginResponse iniciarSesion(LoginRequest request) {
    String identificador = request.identificador().trim();
    Usuario usuario =
        usuarios
            .findByUsernameIgnoreCaseOrEmailIgnoreCase(identificador, identificador)
            .orElseThrow(() -> new IllegalArgumentException("Usuario o contraseña incorrectos"));

    if (usuario.isSuspendido()) {
      throw new IllegalStateException("Esta cuenta está suspendida");
    }
    if (!encoder.matches(request.password(), usuario.getPasswordHash())) {
      throw new IllegalArgumentException("Usuario o contraseña incorrectos");
    }

    Sesion sesion =
        new Sesion(UUID.randomUUID().toString(), usuario.getId(), LocalDateTime.now(), null);
    sesiones.save(sesion);
    return new LoginResponse(sesion.getToken(), usuario, sesion);
  }

  @Transactional
  public void cerrarSesion(String token) {
    Sesion sesion =
        sesiones
            .findByTokenAndCierreIsNull(token)
            .orElseThrow(() -> new EntityNotFoundException("Sesión vigente no encontrada"));
    if (sesion.getCierre() == null) {
      sesion.setCierre(LocalDateTime.now());
    }
    sesiones.save(sesion);
  }

  @Transactional(readOnly = true)
  public Usuario validarToken(String token) {
    Sesion sesion =
        sesiones
            .findByTokenAndCierreIsNull(token)
            .orElseThrow(() -> new IllegalArgumentException("Token inválido o vencido"));
    boolean vigente =
        sesion.getCierre() == null
            && sesion.getInicio() != null
            && !sesion.getInicio().isAfter(LocalDateTime.now());
    if (!vigente) {
      throw new IllegalArgumentException("Token inválido o vencido");
    }
    return buscar(sesion.getUsuarioId());
  }

  @Transactional(readOnly = true)
  public List<Usuario> listar() {
    return usuarios.findAll();
  }

  @Transactional(readOnly = true)
  public Usuario buscar(String id) {
    return usuarios
        .findById(id)
        .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado: " + id));
  }

  @Transactional
  public Usuario actualizarEstado(String id, EstadoUsuarioRequest request) {
    Usuario usuario = buscar(id);
    if (request.suspendido() != null) {
      if (Boolean.TRUE.equals(request.suspendido())) {
        if (request.motivo() == null || request.motivo().isBlank())
          throw new IllegalArgumentException("Indica el motivo de suspensión");
        usuario.setMotivoSuspension(request.motivo());
        usuario.setFechaSuspension(LocalDateTime.now());
      }
      usuario.setSuspendido(request.suspendido());
    }
    if (request.verificado() != null) {
      if (Boolean.TRUE.equals(request.verificado()) && !usuario.isVerificado())
        correo.encolar(
            "VERIFICADA-" + id,
            usuario.getEmail(),
            "Cuenta verificada",
            "Tu cuenta fue verificada. Ya puedes publicar skins.");
      usuario.setVerificado(request.verificado());
    }
    if (request.rol() != null) {
      usuario.setRol(request.rol());
    }
    return usuarios.save(usuario);
  }

  @Transactional
  public Usuario debitarSaldo(String id, BigDecimal monto) {
    Usuario usuario =
        usuarios
            .bloquear(id)
            .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
    validarMontoSaldo(monto);
    if (usuario.isSuspendido()) {
      throw new IllegalStateException("No se puede debitar saldo de una cuenta suspendida");
    }
    if (usuario.getSaldo().compareTo(monto) < 0) {
      throw new IllegalStateException("Saldo insuficiente");
    }
    usuario.setSaldo(usuario.getSaldo().subtract(monto));
    return usuarios.save(usuario);
  }

  @Transactional
  public Usuario abonarSaldo(String id, BigDecimal monto) {
    Usuario usuario =
        usuarios
            .bloquear(id)
            .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
    validarMontoSaldo(monto);
    BigDecimal saldoActual = usuario.getSaldo() == null ? BigDecimal.ZERO : usuario.getSaldo();
    usuario.setSaldo(saldoActual.add(monto));
    return usuarios.save(usuario);
  }

  @Transactional
  public Usuario mover(String id, MovimientoSaldoRequest r, String tipo) {
    Usuario u = usuarios.bloquear(id).orElseThrow();
    validarMontoSaldo(r.monto());
    if (r.referencia() == null || r.referencia().isBlank())
      throw new IllegalArgumentException("Falta la referencia");
    var anterior = movimientos.findByCuentaAndReferencia(id, r.referencia());
    if (anterior.isPresent()) {
      if (!tipo.equals(anterior.get().getTipo())
          || r.monto().compareTo(anterior.get().getMonto()) != 0)
        throw new IllegalStateException("La referencia ya se utilizó para otro movimiento");
      return u;
    }
    if (tipo.equals("DEBITO") && r.referencia().startsWith("REVERSA-")) {
      var venta = movimientos.findByCuentaAndReferencia(id, "VENTA-" + r.referencia().substring(8));
      if (venta.isEmpty()) return u;
      // Una reversa puede dejar deuda si ya se solicitaron las ganancias.
      u.setSaldo(u.getSaldo().subtract(r.monto()));
    } else if (tipo.equals("ABONO")) abonarSaldo(id, r.monto());
    else debitarSaldo(id, r.monto());
    movimientos.save(
        new cl.skinspad.usuario.domain.Movimiento(id, r.referencia(), r.monto(), tipo));
    return u;
  }

  @Transactional
  public Usuario perfil(String id, cl.skinspad.usuario.dto.PerfilRequest r) {
    Usuario u = buscar(id);
    String email = r.email().trim().toLowerCase();
    if (!email.equalsIgnoreCase(u.getEmail()) && usuarios.existsByEmailIgnoreCase(email))
      throw new IllegalArgumentException("El correo ya está registrado");
    u.setEmail(email);
    u.setNombre(r.nombre().trim());
    u.setLinkSteam(r.linkSteam());
    return usuarios.save(u);
  }

  private void validarMontoSaldo(BigDecimal monto) {
    if (monto == null || monto.signum() <= 0) {
      throw new IllegalArgumentException("Monto inválido");
    }
  }
}
