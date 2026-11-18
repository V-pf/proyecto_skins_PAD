package cl.skinspad.usuario.controller;

import cl.skinspad.usuario.domain.Usuario;
import cl.skinspad.usuario.dto.*;
import cl.skinspad.usuario.service.UsuarioService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {
  private final UsuarioService service;

  private final cl.skinspad.usuario.repository.MovimientoRepository movimientos;

  // El retiro acepta pesos enteros y agrega un prefijo a la referencia antes de descontar el saldo.
  @PostMapping("/{id}/retiros")
  public Usuario retirar(@PathVariable String id, @Valid @RequestBody MovimientoSaldoRequest r) {
    if (r.referencia() == null
        || !r.referencia().matches("[0-9a-fA-F-]{36}")
        || r.monto().stripTrailingZeros().scale() > 0)
      throw new IllegalArgumentException("Solicitud de retiro inválida");
    return service.mover(
        id, new MovimientoSaldoRequest(r.monto(), "RETIRO-" + r.referencia()), "RETIRO");
  }

  @GetMapping("/{id}/retiros")
  public List<cl.skinspad.usuario.domain.Movimiento> retiros(@PathVariable String id) {
    return movimientos.findByCuentaAndTipoOrderByFechaDesc(id, "RETIRO");
  }

  @PatchMapping("/{id}/perfil")
  public Usuario perfil(@PathVariable String id, @Valid @RequestBody PerfilRequest r) {
    return service.perfil(id, r);
  }

  private final cl.skinspad.usuario.service.CorreoService correo;

  public record Recibo(String referencia, String detalle) {}

  @PostMapping("/{id}/saldo/recibo")
  public void recibo(@PathVariable String id, @RequestBody Recibo r) {
    var u = service.buscar(id);
    correo.encolar(
        r.referencia(),
        u.getEmail(),
        "Comprobante de compra · Skins PAD",
        "Cliente: " + u.getNombre() + " (" + u.getUsername() + ")\n" + r.detalle());
  }

  // El perfil público solo necesita el identificador, el nombre de usuario, el rol y la verificación.
  public record Publico(String id, String username, String rol, boolean verificado) {}

  @GetMapping("/publicos")
  public List<Publico> publicos() {
    return service.listar().stream()
        .map(u -> new Publico(u.getId(), u.getUsername(), u.getRol().name(), u.isVerificado()))
        .toList();
  }

  @GetMapping
  public List<Usuario> listar() {
    return service.listar();
  }

  @GetMapping("/{id}")
  public Usuario buscar(@PathVariable String id) {
    return service.buscar(id);
  }

  @PatchMapping("/{id}/estado")
  public Usuario estado(@PathVariable String id, @RequestBody EstadoUsuarioRequest r) {
    return service.actualizarEstado(id, r);
  }

  @PostMapping("/{id}/saldo/debitar")
  public Usuario debitar(@PathVariable String id, @Valid @RequestBody MovimientoSaldoRequest r) {
    return service.mover(id, r, "DEBITO");
  }

  @PostMapping("/{id}/saldo/abonar")
  public Usuario abonar(@PathVariable String id, @Valid @RequestBody MovimientoSaldoRequest r) {
    return service.mover(id, r, "ABONO");
  }
}
