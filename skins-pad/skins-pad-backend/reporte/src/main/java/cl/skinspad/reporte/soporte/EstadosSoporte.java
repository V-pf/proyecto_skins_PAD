package cl.skinspad.reporte.soporte;
import java.util.Set;
public final class EstadosSoporte {
 private EstadosSoporte(){}
 public enum Estado { ABIERTO, EN_REVISION, RESUELTO, RECHAZADO, CERRADO }
 public enum Categoria { CUENTA, PAGO, SALDO, PUBLICACION, ERROR_TECNICO, OTRO }
 public enum Objetivo { PUBLICACION, OPERACION, USUARIO }
 public enum Motivo { PRECIO_EXORBITANTE, PUBLICACION_FALSA, SKIN_NO_COINCIDE_CON_DESCRIPCION, FRAUDE_EN_OPERACION, SUPLANTACION, OTRO }
 public enum Accion { SIN_ACCION, ADVERTENCIA, PUBLICACION_DESPUBLICADA, OPERACION_CONGELADA, OPERACION_REEMBOLSADA, USUARIO_SUSPENDIDO }
 public enum Prioridad { NORMAL, ALTA, URGENTE }
 public static boolean abierto(Estado e){return e==Estado.ABIERTO||e==Estado.EN_REVISION;}
 public static void transicion(Estado a,Estado b,boolean ticket,boolean admin){
  boolean valida=admin && abierto(a) && b!=null && (b==Estado.EN_REVISION&&a==Estado.ABIERTO||b==Estado.RESUELTO||b==Estado.RECHAZADO);
  if(ticket&&b==Estado.CERRADO&&(abierto(a)||a==Estado.RESUELTO||a==Estado.RECHAZADO))valida=true;
  if(!valida)throw new IllegalStateException("Transición no permitida: "+a+" → "+b);
 }
 public static String simple(Estado e){return abierto(e)?"En atención":e==Estado.CERRADO?"Cerrado":"Finalizado";}
}
