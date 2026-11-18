package cl.skinspad.usuario.config;

import cl.skinspad.usuario.domain.Rol;
import cl.skinspad.usuario.domain.Usuario;
import cl.skinspad.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UsuarioSeedTest {
    @Test void creaVendedorVerificadoEnUnaBaseDemoExistente() throws Exception {
        var repo = mock(UsuarioRepository.class);
        var encoder = mock(PasswordEncoder.class);
        when(encoder.encode("Demo1234!")).thenReturn("hash-demo");
        new UsuarioSeed().cargarVendedorDemo(repo, encoder).run();
        var captura = ArgumentCaptor.forClass(Usuario.class);
        verify(repo).save(captura.capture());
        var u = captura.getValue();
        assertEquals("vendedor_demo", u.getUsername());
        assertEquals(Rol.cliente, u.getRol());
        assertTrue(u.isVerificado());
        assertFalse(u.isSuspendido());
        assertEquals("hash-demo", u.getPasswordHash());
    }
    @Test void noRestableceLaCuentaSiYaExiste() throws Exception {
        var repo = mock(UsuarioRepository.class);
        var encoder = mock(PasswordEncoder.class);
        when(repo.existsByUsernameIgnoreCase("vendedor_demo")).thenReturn(true);
        new UsuarioSeed().cargarVendedorDemo(repo, encoder).run();
        verify(repo, never()).save(any());
        verifyNoInteractions(encoder);
    }
}
