package cl.skinspad.pago.escrow;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static cl.skinspad.pago.escrow.EstadoOperacion.*;
import static cl.skinspad.pago.escrow.MaquinaOperacion.Accion.*;
class MaquinaOperacionTest {
 @Test void feliz(){var e=MaquinaOperacion.siguiente(PAGO_PENDIENTE,VERIFICAR);e=MaquinaOperacion.siguiente(e,ACEPTAR);e=MaquinaOperacion.siguiente(e,ENTREGAR);assertEquals(COMPLETADA,MaquinaOperacion.siguiente(e,RECIBIR));}
 @Test void sinSaltos(){for(var e:EstadoOperacion.values())for(var a:MaquinaOperacion.Accion.values()) {try{var n=MaquinaOperacion.siguiente(e,a);assertNotEquals(e,n);assertFalse(MaquinaOperacion.terminal(e));}catch(IllegalStateException expected){}} assertThrows(IllegalStateException.class,()->MaquinaOperacion.siguiente(PAGO_RETENIDO,RECIBIR));}
 @Test void disputaDetieneLiberacion(){assertThrows(IllegalStateException.class,()->MaquinaOperacion.siguiente(EN_DISPUTA,AUTO_LIBERAR));assertThrows(IllegalStateException.class,()->MaquinaOperacion.siguiente(CONGELADA,AUTO_LIBERAR));assertEquals(REEMBOLSADA,MaquinaOperacion.siguiente(EN_DISPUTA,RESOLVER_COMPRADOR));assertEquals(COMPLETADA,MaquinaOperacion.siguiente(CONGELADA,RESOLVER_VENDEDOR));}
 @Test void rechazosYPlazos(){assertEquals(REEMBOLSADA,MaquinaOperacion.siguiente(PAGO_RETENIDO,RECHAZAR));assertEquals(REEMBOLSADA,MaquinaOperacion.siguiente(PAGO_RETENIDO,EXPIRAR));assertEquals(CANCELADA,MaquinaOperacion.siguiente(PAGO_PENDIENTE,EXPIRAR));assertEquals(COMPLETADA,MaquinaOperacion.siguiente(ENTREGADA,AUTO_LIBERAR));}
}
