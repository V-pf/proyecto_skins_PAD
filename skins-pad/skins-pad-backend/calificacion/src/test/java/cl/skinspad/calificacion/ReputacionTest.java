package cl.skinspad.calificacion;

import cl.skinspad.calificacion.domain.Resena;
import cl.skinspad.calificacion.service.CalificacionService;
import java.math.BigDecimal;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ReputacionTest {
    private CalificacionService.Reputacion reputacion(String... valores) {
        return CalificacionService.calcular(Arrays.stream(valores).map(valor -> {
            var resena = new Resena();
            resena.setPuntaje(new BigDecimal(valor));
            return resena;
        }).toList());
    }

    @Test void sinResenasNoTienePuntajeNiEtiqueta() {
        assertNull(reputacion().promedio());
        assertFalse(reputacion().trustedSeller());
    }

    @Test void requiereSuperarCuatro() {
        assertFalse(reputacion("4.0").trustedSeller());
        assertTrue(reputacion("4.01").trustedSeller());
    }

    @Test void conservaLaEtiquetaHastaTresPuntoCincoInclusive() {
        assertTrue(reputacion("5", "2").trustedSeller());
        assertFalse(reputacion("5", "1.99").trustedSeller());
    }

    @Test void dependeDelHistorialYPuedeRecuperarse() {
        assertFalse(reputacion("3.5").trustedSeller());
        assertTrue(reputacion("5", "1", "5", "5", "5").trustedSeller());
    }

    @Test void comparaSinRedondearElPromedio() {
        assertTrue(reputacion("4.01", "4", "4").trustedSeller());
        assertEquals(new BigDecimal("4.00"), reputacion("4.01", "4", "4").promedio());
    }
}
