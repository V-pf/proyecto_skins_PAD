package cl.skinspad.reporte.soporte;
import java.util.*;import lombok.RequiredArgsConstructor;import org.springframework.stereotype.Service;
@Service @RequiredArgsConstructor public class ReporteAcciones {
 private final ReporteRepository reportes;private final ReporteService service;private final SoporteCatalogoClient catalogo;private final SoporteUsuarioClient usuarios;private final PagoClient pagos;
 public void ejecutar(Long id){var r=reportes.findById(id).orElseThrow();if(!r.isAccionPendiente())return;String ref=r.getReferenciaAccion();try{
  var actor=usuarios.obtener(r.getAdminResolutorId());if(!"admin".equals(actor.rol())||actor.suspendido())throw new IllegalStateException("El administrador ya no está habilitado");
  String motivo=ref+" · Moderación de soporte: "+r.getAccionTomada();var orden=new SoporteCatalogoClient.Orden(ref,r.getAdminResolutorId(),motivo);
  switch(r.getAccionTomada()){
   case SIN_ACCION,ADVERTENCIA -> {}
   case PUBLICACION_DESPUBLICADA -> catalogo.despublicar(Long.valueOf(r.getObjetivoId()),orden);
   case USUARIO_SUSPENDIDO -> usuarios.suspender(r.getDenunciadoId(),orden);
   case OPERACION_CONGELADA,OPERACION_REEMBOLSADA -> pagos.moderar(Long.valueOf(r.getObjetivoId()), new PagoClient.Orden(r.getAccionTomada().name(),r.getAdminResolutorId(),motivo));
  }
  service.finalizar(id,ref);
 }catch(feign.FeignException e){if(java.util.Set.of(400,404,409).contains(e.status())){service.noEjecutada(id,ref);return;}service.fallo(id,ref,e.status()==409?"El objetivo cambió o está reservado; revisa el caso antes de reintentar":"El servicio dependiente no confirmó la acción; se reintentará sin duplicarla");}catch(IllegalStateException e){
  service.fallo(id,ref,"Acción pendiente de conciliación; no se ha cerrado el reporte");
 }catch(RuntimeException e){service.fallo(id,ref,"La acción no está confirmada. Revisa el estado y reintenta; no se ha cerrado el reporte");}}
 @org.springframework.scheduling.annotation.Scheduled(fixedDelayString="${soporte.reintento-ms:60000}",initialDelayString="${soporte.job-inicial-ms:60000}") public void reintentar(){for(var r:reportes.findByAccionPendienteTrue())ejecutar(r.getId());}
}
