package cl.skinspad.pago.escrow;
import cl.skinspad.pago.stats.StatsUserClient;
import cl.skinspad.pago.repository.PagoRepository;
import cl.skinspad.pago.domain.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
@RestController @RequestMapping("/api/operaciones") @RequiredArgsConstructor
public class OperacionController {
 private final OperacionRepository operaciones;private final EventoRepository eventos;private final PagoRepository pagos;private final CustodiaService service;private final CustodiaTransactions tx;private final StatsUserClient users;private final cl.skinspad.pago.service.PagoService legacy;
 public record Peticion(String motivo){}
 public record Vista(Operacion operacion,List<EventoOperacion> eventos,String metodo,boolean pagoAtascado){}
 private StatsUserClient.Identity identidad(String auth){
   if(auth==null || !auth.startsWith("Bearer "))throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
   StatsUserClient.Identity u;try{u=users.validate(auth.substring(7));}catch(feign.FeignException e){throw new ResponseStatusException(e.status()<0||e.status()>=500?HttpStatus.SERVICE_UNAVAILABLE:HttpStatus.UNAUTHORIZED);}if(u.suspendido())throw new ResponseStatusException(HttpStatus.FORBIDDEN);return u;
 }
 private boolean admin(StatsUserClient.Identity u){return "admin".equals(u.rol());}
 private void acceso(Operacion o,StatsUserClient.Identity u){if(!admin(u)&&!u.id().equals(o.getCompradorId())&&!u.id().equals(o.getVendedorId()))throw new ResponseStatusException(HttpStatus.FORBIDDEN);}
 private Vista vista(Operacion o){var p=pagos.findById(o.getPagoId()).orElseThrow();return new Vista(o,eventos.findByOperacionIdOrderById(o.getId()),p.getMetodo().name(),p.getErrorCustodia()!=null);}
 @GetMapping public List<Vista> listar(@RequestHeader(value="Authorization",required=false)String auth,@RequestParam(defaultValue="compras")String vista,@RequestParam(required=false)EstadoOperacion estado){
   var u=identidad(auth);List<Operacion> os;
   if(admin(u)){os=operaciones.findAll();if(!vista.equals("historial"))os=os.stream().filter(o->!o.isHistorica() && (o.getEstado()==EstadoOperacion.EN_DISPUTA || o.getEstado()==EstadoOperacion.CONGELADA || o.getEstado()==EstadoOperacion.PAGO_PENDIENTE || !o.isSincronizado() || pagos.findById(o.getPagoId()).orElseThrow().getErrorCustodia()!=null)).toList();}
   else os=vista.equals("ventas")?operaciones.findByVendedorIdOrderByIdDesc(u.id()):operaciones.findByCompradorIdOrderByIdDesc(u.id());
   return os.stream().filter(o->estado==null||o.getEstado()==estado).sorted(Comparator.comparing(Operacion::getId).reversed()).map(this::vista).toList();
 }
 @GetMapping("/{id}") public Vista obtener(@PathVariable Long id,@RequestHeader(value="Authorization",required=false)String auth){var o=operaciones.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));acceso(o,identidad(auth));return vista(o);}
 @PostMapping("/{id}/{accion}") public Vista accion(@PathVariable Long id,@PathVariable String accion,@RequestHeader(value="Authorization",required=false)String auth,@RequestBody(required=false)Peticion body){
   var u=identidad(auth);var o=operaciones.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));acceso(o,u);
   var a=switch(accion){case "aceptar"->MaquinaOperacion.Accion.ACEPTAR;case "rechazar"->MaquinaOperacion.Accion.RECHAZAR;case "entregar"->MaquinaOperacion.Accion.ENTREGAR;case "recibir"->MaquinaOperacion.Accion.RECIBIR;case "disputar"->MaquinaOperacion.Accion.DISPUTAR;case "congelar"->MaquinaOperacion.Accion.CONGELAR;case "resolver-comprador"->MaquinaOperacion.Accion.RESOLVER_COMPRADOR;case "resolver-vendedor"->MaquinaOperacion.Accion.RESOLVER_VENDEDOR;case "cancelar"->MaquinaOperacion.Accion.CANCELAR;default->throw new IllegalArgumentException("Acción inválida");};
   service.accion(id,a,u.id(),u.rol(),body==null?null:body.motivo(),false);return vista(operaciones.findById(id).orElseThrow());
 }
 @PostMapping("/pagos/{id}/verificar") public List<Vista> verificar(@PathVariable Long id,@RequestHeader(value="Authorization",required=false)String auth,@RequestBody Peticion body){
   var u=identidad(auth);if(!admin(u))throw new ResponseStatusException(HttpStatus.FORBIDDEN);
   if(body.motivo()==null||body.motivo().isBlank()||body.motivo().length()>1000)throw new IllegalArgumentException("Indica cómo verificaste el pago");
   var p=pagos.findById(id).orElseThrow();
   if(p.getMetodo()==MetodoPago.SALDO)throw new IllegalStateException("El saldo se verifica automáticamente");
   if(p.getMetodo()==MetodoPago.TRANSFERENCIA && !legacy.comprobante(id).isVerificado())throw new IllegalStateException("Verifica primero el comprobante");
   tx.verificarAdministrativamente(id,u.id(),body.motivo());service.pago(id);return operaciones.findByPagoIdOrderById(id).stream().map(this::vista).toList();
 }
}
