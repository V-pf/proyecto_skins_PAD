package cl.skinspad.reporte.soporte;
import lombok.RequiredArgsConstructor;import org.springframework.stereotype.Service;import static cl.skinspad.reporte.soporte.EstadosSoporte.*;
@Service @RequiredArgsConstructor public class SoporteContexto {
 private final SoporteCatalogoClient catalogo;private final SoporteUsuarioClient usuarios;private final PagoClient operaciones;
 public PagoClient.Operacion operacion(Long id){return operaciones.obtener(id);}
 public void participante(PagoClient.Operacion o,SoporteIdentidad.Actor a){if(!a.admin()&&!o.compradorId().equals(a.id())&&!o.vendedorId().equals(a.id()))throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN,"Solo puedes referenciar operaciones propias");}
 public void referencias(Long op,Long arma,SoporteIdentidad.Actor a){if(op!=null){var o=operacion(op);participante(o,a);if(arma!=null&&!arma.equals(o.productoId()))throw new IllegalArgumentException("La publicación no pertenece a esta operación");}if(arma!=null)catalogo.obtener(arma);}
 public record Destino(String id,String denunciado,boolean enCurso){}
 public Destino objetivo(Objetivo tipo,String id,SoporteIdentidad.Actor a){if(tipo==null)throw new IllegalArgumentException("Tipo de objetivo obligatorio");String owner;boolean activo=false;
  if(tipo==Objetivo.PUBLICACION){var p=catalogo.obtener(ValidacionSoporte.numero(id));id=p.id().toString();owner=p.vendedorId();}
  else if(tipo==Objetivo.USUARIO){var u=usuarios.obtener(ValidacionSoporte.id(id));owner=u.id();id=u.id();}
  else{var o=operacion(ValidacionSoporte.numero(id));participante(o,a);id=o.id().toString();owner=a.id().equals(o.compradorId())?o.vendedorId():o.compradorId();activo=!o.terminal();}
  if(owner==null||owner.equals(a.id()))throw new IllegalArgumentException("No puedes reportarte ni reportar tu propia publicación");return new Destino(id,owner,activo);
 }
 public void adminAsignable(String id){if(id==null)return;var a=usuarios.obtener(ValidacionSoporte.id(id));if(!"admin".equals(a.rol())||a.suspendido())throw new IllegalArgumentException("El asignado debe ser un administrador activo");}

 public void validarAccion(Reporte r,Accion accion){
  if(accion==Accion.PUBLICACION_DESPUBLICADA){var p=catalogo.obtener(Long.valueOf(r.getObjetivoId()));if(p.reservada()||!java.util.Set.of("ACTIVO","PENDIENTE_APROBACION").contains(p.estado()))throw new IllegalStateException("No se puede despublicar: estado incompatible o reserva activa");}
  if(accion==Accion.USUARIO_SUSPENDIDO&&"admin".equals(usuarios.obtener(r.getDenunciadoId()).rol()))throw new IllegalStateException("No se puede suspender un administrador desde reportes");
  if(accion==Accion.OPERACION_CONGELADA||accion==Accion.OPERACION_REEMBOLSADA){var o=operacion(Long.valueOf(r.getObjetivoId()));if(o.historica()||o.accionPendiente()!=null)throw new IllegalStateException("Operación histórica o en recuperación");if(!(accion==Accion.OPERACION_CONGELADA?o.congelable():o.reembolsable()))throw new IllegalStateException("Transición no permitida");}
 }
}
