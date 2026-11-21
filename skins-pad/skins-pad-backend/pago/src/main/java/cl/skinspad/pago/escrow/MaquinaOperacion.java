package cl.skinspad.pago.escrow;
import java.util.*;
public final class MaquinaOperacion {
  private MaquinaOperacion() {}
  public enum Accion { VERIFICAR, ACEPTAR, RECHAZAR, ENTREGAR, RECIBIR, DISPUTAR, CONGELAR, RESOLVER_COMPRADOR, RESOLVER_VENDEDOR, EXPIRAR, AUTO_LIBERAR, CANCELAR }
  public static EstadoOperacion siguiente(EstadoOperacion e, Accion a) {return siguiente(e,a,true);}
  public static EstadoOperacion siguiente(EstadoOperacion e, Accion a, boolean fondos) {
    var s = switch(a) {
      case VERIFICAR -> e==EstadoOperacion.PAGO_PENDIENTE ? EstadoOperacion.PAGO_RETENIDO : null;
      case ACEPTAR -> e==EstadoOperacion.PAGO_RETENIDO ? EstadoOperacion.ACEPTADA_POR_VENDEDOR : null;
      case ENTREGAR -> e==EstadoOperacion.ACEPTADA_POR_VENDEDOR ? EstadoOperacion.ENTREGADA : null;
      case RECIBIR, AUTO_LIBERAR -> e==EstadoOperacion.ENTREGADA ? EstadoOperacion.COMPLETADA : null;
      case RECHAZAR -> e==EstadoOperacion.PAGO_RETENIDO ? EstadoOperacion.REEMBOLSADA : null;
      case EXPIRAR -> e==EstadoOperacion.PAGO_PENDIENTE ? EstadoOperacion.CANCELADA :
        (e==EstadoOperacion.PAGO_RETENIDO || e==EstadoOperacion.ACEPTADA_POR_VENDEDOR) ? EstadoOperacion.REEMBOLSADA : null;
      case CANCELAR -> e==EstadoOperacion.PAGO_PENDIENTE ? EstadoOperacion.CANCELADA : null;
      case DISPUTAR -> e==EstadoOperacion.ENTREGADA || e==EstadoOperacion.ACEPTADA_POR_VENDEDOR ? EstadoOperacion.EN_DISPUTA : null;
      case CONGELAR -> !terminal(e) && e!=EstadoOperacion.CONGELADA ? EstadoOperacion.CONGELADA : null;
      case RESOLVER_COMPRADOR -> e==EstadoOperacion.EN_DISPUTA || e==EstadoOperacion.CONGELADA ? EstadoOperacion.REEMBOLSADA : null;
      case RESOLVER_VENDEDOR -> e==EstadoOperacion.EN_DISPUTA || e==EstadoOperacion.CONGELADA ? EstadoOperacion.COMPLETADA : null;
    };
    if(s==null) throw new IllegalStateException("Transición no permitida: "+e+" → "+a);
    if(s==EstadoOperacion.COMPLETADA && !fondos)throw new IllegalStateException("No hay fondos retenidos");
    return s==EstadoOperacion.REEMBOLSADA && !fondos?EstadoOperacion.CANCELADA:s;
  }
  public static boolean terminal(EstadoOperacion e) {return EnumSet.of(EstadoOperacion.COMPLETADA,EstadoOperacion.REEMBOLSADA,EstadoOperacion.CANCELADA).contains(e);}
}
