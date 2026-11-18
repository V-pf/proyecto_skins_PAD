package cl.skinspad.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import static org.junit.jupiter.api.Assertions.*;

class AccesoFilterTest {
    private final AccesoFilter filtro = new AccesoFilter(new ObjectMapper());
    private boolean permite(String ruta, String metodo, String rol) {
        Mono<Boolean> resultado = ReflectionTestUtils.invokeMethod(filtro, "permitido",
            null, ruta, metodo, Map.of("id", "u1", "rol", rol, "username", "cuenta"), Map.of(), "token");
        return Boolean.TRUE.equals(resultado.block());
    }
    @Test void verificarPagoSoloAdminYPost(){assertTrue(permite("/api/operaciones/pagos/1/verificar","POST","admin"));assertFalse(permite("/api/operaciones/pagos/1/verificar","POST","cliente"));assertFalse(permite("/api/operaciones/pagos/1/verificar","GET","admin"));}
    @Test void estadisticasSoloPermiteGetAdministrativo() {
        assertTrue(permite("/api/admin/stats", "GET", "admin"));
        assertFalse(permite("/api/admin/stats", "GET", "cliente"));
        assertFalse(permite("/api/admin/stats", "POST", "admin"));
    }
    @Test void administradorNoPuedeComprarVenderNiRetirarSaldo() {
        assertFalse(permite("/api/pagos/checkout", "POST", "admin"));
        assertFalse(permite("/api/productos", "POST", "admin"));
        assertFalse(permite("/api/carritos/usuario/u1", "GET", "admin"));
        assertFalse(permite("/api/usuarios/u1/retiros", "POST", "admin"));
        assertFalse(permite("/api/pagos/mis-ventas", "GET", "admin"));
    }
    @Test void administradorConservaGestionesYClientePuedeComprar() {
        assertTrue(permite("/api/usuarios/u1/estado", "PATCH", "admin"));
        assertTrue(permite("/api/productos/1", "DELETE", "admin"));
        assertFalse(permite("/api/pagos/1/confirmar", "POST", "admin"));
        assertTrue(permite("/api/categorias", "POST", "admin"));
        assertTrue(permite("/api/pagos/checkout", "POST", "cliente"));
    }
}
