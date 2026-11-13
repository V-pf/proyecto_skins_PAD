package cl.skinspad.usuario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cl.skinspad.usuario.domain.Sesion;
import cl.skinspad.usuario.domain.Usuario;
import cl.skinspad.usuario.repository.SesionRepository;
import cl.skinspad.usuario.repository.UsuarioRepository;
import cl.skinspad.usuario.service.UsuarioService;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarios;

    @Mock
    private SesionRepository sesiones;

    @Mock
    private PasswordEncoder encoder;

    @Mock private cl.skinspad.usuario.repository.MovimientoRepository movimientos;

    @InjectMocks
    private UsuarioService service;

    @Test
    void validaTokenDeSesionVigente() {
        Sesion sesion = new Sesion("token", "u1", LocalDateTime.now().minusMinutes(1), null);
        Usuario usuario = new Usuario();
        usuario.setId("u1");
        when(sesiones.findByTokenAndCierreIsNull("token")).thenReturn(Optional.of(sesion));
        when(usuarios.findById("u1")).thenReturn(Optional.of(usuario));

        Usuario resultado = service.validarToken("token");

        assertEquals("u1", resultado.getId());
    }

    @Test
    void rechazaTokenConInicioEnElFuturo() {
        Sesion sesion = new Sesion("token", "u1", LocalDateTime.now().plusMinutes(3), null);
        when(sesiones.findByTokenAndCierreIsNull("token")).thenReturn(Optional.of(sesion));

        assertThrows(IllegalArgumentException.class, () -> service.validarToken("token"));
    }

    @Test
    void cerrarSesionRegistraLaFechaDeCierreEnElServicio() {
        Sesion sesion = new Sesion("token", "u1", LocalDateTime.now().minusMinutes(1), null);
        when(sesiones.findByTokenAndCierreIsNull("token")).thenReturn(Optional.of(sesion));
        when(sesiones.save(sesion)).thenReturn(sesion);

        service.cerrarSesion("token");

        verify(sesiones).save(sesion);
        org.junit.jupiter.api.Assertions.assertNotNull(sesion.getCierre());
    }

    @Test void retiroDuplicadoDescuentaUnaVez(){
        var monto=new java.math.BigDecimal("500");
        Usuario u=new Usuario();u.setId("u1");u.setSaldo(monto);
        when(usuarios.bloquear("u1")).thenReturn(Optional.of(u));
        var movimiento=new cl.skinspad.usuario.domain.Movimiento("u1","RETIRO-x",monto,"RETIRO");
        when(movimientos.findByCuentaAndReferencia("u1","RETIRO-x")).thenReturn(Optional.empty(),Optional.of(movimiento));
        var solicitud=new cl.skinspad.usuario.dto.MovimientoSaldoRequest(monto,"RETIRO-x");
        service.mover("u1",solicitud,"RETIRO");service.mover("u1",solicitud,"RETIRO");
        assertEquals(java.math.BigDecimal.ZERO,u.getSaldo());
        verify(movimientos).save(org.mockito.ArgumentMatchers.any());
    }
}
