package cl.skinspad.reporte.soporte;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
@FeignClient(name="custodiaSoporte",url="${PAGO_URL:http://localhost:8084}")
public interface PagoClient {
 record Operacion(Long id,Long productoId,String compradorId,String vendedorId,String estado,
                  boolean retenido,boolean historica,String accionPendiente,boolean terminal,
                  boolean congelable,boolean reembolsable){}
 @GetMapping("/api/internal/custodia/{id}") Operacion obtener(@PathVariable("id") Long id);
 record Orden(String accion,String actor,String motivo){}
 @PostMapping("/api/internal/custodia/{id}") void moderar(@PathVariable("id") Long id,@RequestBody Orden orden);
}
