package cl.skinspad.pago.escrow;
import cl.skinspad.pago.config.InternalApiKey;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import static cl.skinspad.pago.escrow.MaquinaOperacion.Accion.*;
@RestController
@RequestMapping("/api/internal/custodia")
@RequiredArgsConstructor
public class CustodiaSoporteController {
 private final InternalApiKey key;
 private final OperacionRepository operaciones;
 private final EventoRepository eventos;
 private final CustodiaService custodia;
 private void autenticar(String credencial) {
  if(!key.value().equals(credencial)) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
 }
 record Vista(Long id,Long productoId,String compradorId,String vendedorId,String estado,boolean retenido,
              boolean historica,String accionPendiente,boolean terminal,boolean congelable,boolean reembolsable){}
 record Orden(String accion,String actor,String motivo){}
 private boolean permite(Operacion o,MaquinaOperacion.Accion accion){
  try { MaquinaOperacion.siguiente(o.getEstado(),accion,o.isRetenido()); return true; }
  catch(IllegalStateException e){return false;}
 }
 @GetMapping("/{id}")
 Vista obtener(@RequestHeader("X-Skins-Internal") String clave,@PathVariable Long id){
  autenticar(clave);
  var o=operaciones.findById(id).orElseThrow(()->new EntityNotFoundException("Operación no encontrada"));
  return new Vista(id,o.getProductoId(),o.getCompradorId(),o.getVendedorId(),o.getEstado().name(),
    o.isRetenido(),o.isHistorica(),o.getAccionPendiente(),MaquinaOperacion.terminal(o.getEstado()),
    permite(o,CONGELAR),permite(o,RESOLVER_COMPRADOR));
 }
 private boolean confirmada(Long id,MaquinaOperacion.Accion accion,String motivo){
  return eventos.findByOperacionIdOrderById(id).stream().anyMatch(e->accion.name().equals(e.getAccion())&&motivo.equals(e.getMotivo()));
 }
 @PostMapping("/{id}")
 void moderar(@RequestHeader("X-Skins-Internal") String clave,@PathVariable Long id,@RequestBody Orden orden){
  autenticar(clave);
  var accion=switch(orden.accion()){
   case "OPERACION_CONGELADA" -> CONGELAR;
   case "OPERACION_REEMBOLSADA" -> RESOLVER_COMPRADOR;
   default -> throw new IllegalArgumentException("Acción inválida");
  };
  if(confirmada(id,accion,orden.motivo()))return;
  var o=operaciones.findById(id).orElseThrow(()->new EntityNotFoundException("Operación no encontrada"));
  if(o.getAccionPendiente()!=null&&!orden.motivo().equals(o.getMotivoPendiente()))throw new IllegalStateException("Existe otra acción pendiente");
  try {custodia.accion(id,accion,orden.actor(),"admin",orden.motivo(),false);}
  catch(IllegalStateException e){
   if(confirmada(id,accion,orden.motivo()))return;
   var actual=operaciones.findById(id).orElseThrow();
   if(!orden.motivo().equals(actual.getMotivoPendiente()))throw e;
  }
  if(!confirmada(id,accion,orden.motivo()))throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Conciliación pendiente");
 }
}
