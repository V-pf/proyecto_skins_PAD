package cl.skinspad.ticket.soporte;
import org.junit.jupiter.api.Test;import static org.junit.jupiter.api.Assertions.*;import static cl.skinspad.ticket.soporte.EstadosSoporte.*;
class EstadosSoporteTest {
 @Test void transiciones(){transicion(Estado.ABIERTO,Estado.EN_REVISION,false,true);transicion(Estado.EN_REVISION,Estado.RESUELTO,false,true);transicion(Estado.ABIERTO,Estado.CERRADO,true,false);assertThrows(IllegalStateException.class,()->transicion(Estado.ABIERTO,Estado.RESUELTO,false,false));assertThrows(IllegalStateException.class,()->transicion(Estado.RESUELTO,Estado.ABIERTO,true,true));assertThrows(IllegalStateException.class,()->transicion(Estado.ABIERTO,Estado.CERRADO,false,true));}
}
