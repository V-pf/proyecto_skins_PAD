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
    @Test void clienteYAdminNoPuedenVerificarPagos() {
        for (String rol : java.util.List.of("admin", "cliente")) {
            assertFalse(permite("/api/operaciones/pagos/1/verificar", "POST", rol));
            assertFalse(permite("/api/pagos/1/comprobante/verificar", "POST", rol));
        }
    }
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

 @Test void soporteProtegeRutasAdministrativas(){for(String ruta:java.util.List.of("/api/admin/tickets","/api/admin/reportes/1/resolver","/api/admin/soporte/stats","/api/admin/soporte/precios")){assertFalse(permite(ruta,"GET","cliente"));assertFalse(permite(ruta,"POST","cliente"));assertTrue(permite(ruta,"GET","admin"));}assertTrue(permite("/api/tickets","POST","cliente"));assertFalse(permite("/api/tickets","POST","admin"));assertTrue(permite("/api/reportes/mis-reportes","GET","cliente"));}
}
