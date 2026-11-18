package cl.skinspad.pago.escrow;
import lombok.RequiredArgsConstructor;import org.springframework.stereotype.Component;import org.springframework.scheduling.annotation.Scheduled;import java.time.Instant;
@Component @RequiredArgsConstructor
public class CustodiaJob {
 private final OperacionRepository operaciones;private final cl.skinspad.pago.repository.PagoRepository pagos;private final CustodiaService service;
 private final cl.skinspad.pago.config.CustodiaSeed inicializador;
 private static final org.slf4j.Logger log=org.slf4j.LoggerFactory.getLogger(CustodiaJob.class);
 @Scheduled(fixedDelayString="${escrow.job-ms:30000}",initialDelayString="${escrow.job-initial-ms:30000}")
 public void ejecutar(){
   inicializador.cargarPendientes();
   for(var p:pagos.findByCustodiaTrueAndEtapaCustodiaNot("LISTO")){try{service.pago(p.getId());}catch(RuntimeException e){log.warn("Pago {} pendiente de recuperación",p.getId());}}
   for(var id:operaciones.pendientes(Instant.now(),org.springframework.data.domain.PageRequest.of(0,100))){try{
     var o=operaciones.findById(id).orElseThrow();
     if(o.getAccionPendiente()!=null)service.procesar(id);
     else service.accion(id,o.getEstado()==EstadoOperacion.ENTREGADA?MaquinaOperacion.Accion.AUTO_LIBERAR:MaquinaOperacion.Accion.EXPIRAR,"SISTEMA","sistema","Plazo vencido",true);
   }catch(RuntimeException e){log.warn("Operación {} pendiente de recuperación",id);}}
 }
}
