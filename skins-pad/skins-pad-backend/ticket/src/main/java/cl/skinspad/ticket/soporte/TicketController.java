package cl.skinspad.ticket.soporte;
import lombok.RequiredArgsConstructor;import org.springframework.web.bind.annotation.*;import com.fasterxml.jackson.databind.JsonNode;import static cl.skinspad.ticket.soporte.SoporteDtos.*;
@RestController @RequestMapping("/api") @RequiredArgsConstructor public class TicketController {
 private final TicketService service;private final SoporteIdentidad identidad;private final SoporteCuotas cuotas;private final SoporteJson json;
 public record Cierre(Long version,String motivo){}
 @PostMapping("/tickets") @ResponseStatus(org.springframework.http.HttpStatus.CREATED) public TicketVista crear(@RequestHeader(value="Authorization",required=false)String auth,@RequestBody JsonNode b){var a=identidad.validar(auth,false);SoporteIdentidad.cliente(a);var r=json.leer(b,NuevoTicket.class);cuotas.preparar(a.id());return service.crear(r,a);}
 @GetMapping("/tickets/mis-tickets") public java.util.List<TicketVista> mios(@RequestHeader(value="Authorization",required=false)String auth){return service.mios(identidad.validar(auth,false));}
 @GetMapping("/tickets/{id}") public TicketVista obtener(@PathVariable Long id,@RequestHeader(value="Authorization",required=false)String auth){return service.obtener(id,identidad.validar(auth,false));}
 @PostMapping("/tickets/{id}/mensajes") public TicketVista mensaje(@PathVariable Long id,@RequestHeader(value="Authorization",required=false)String auth,@RequestBody JsonNode b){var a=identidad.validar(auth,false);return service.responder(id,json.leer(b,Mensaje.class),a);}
 @PostMapping("/tickets/{id}/cerrar") public TicketVista cerrar(@PathVariable Long id,@RequestHeader(value="Authorization",required=false)String auth,@RequestBody JsonNode b){var a=identidad.validar(auth,false);var r=json.leer(b,Cierre.class);return service.cambiar(id,new Cambio(r.version(),EstadosSoporte.Estado.CERRADO,null,r.motivo()),a);}
 @PatchMapping("/admin/tickets/{id}/estado") public TicketVista estado(@PathVariable Long id,@RequestHeader(value="Authorization",required=false)String auth,@RequestBody JsonNode b){var a=identidad.validar(auth,true);return service.cambiar(id,json.leer(b,Cambio.class),a);}
 @PostMapping("/admin/tickets/{id}/asignar") public TicketVista asignar(@PathVariable Long id,@RequestHeader(value="Authorization",required=false)String auth,@RequestBody JsonNode b){var a=identidad.validar(auth,true);return service.asignar(id,json.leer(b,Asignacion.class),a);}
}
