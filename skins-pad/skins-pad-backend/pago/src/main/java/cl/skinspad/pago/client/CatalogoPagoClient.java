package cl.skinspad.pago.client;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.math.BigDecimal;
@FeignClient(name="catalogoPago",url="${CATALOGO_URL:http://localhost:8081}")
public interface CatalogoPagoClient {
 record Skin(Long id,String vendedorId,BigDecimal precio,String nombre,Boolean demo){
  public Skin(Long id,String vendedor,BigDecimal precio){this(id,vendedor,precio,null,null);}
  public Skin(Long id,String vendedor,BigDecimal precio,String nombre){this(id,vendedor,precio,nombre,null);}
 }
 record Reserva(String referencia,List<Long> ids,String accion){}
 @GetMapping("/api/productos/vendedor/{id}") List<Skin> porVendedor(@PathVariable("id") String id);
 @GetMapping("/api/productos/{id}") Skin buscar(@PathVariable("id") Long id);
 @PostMapping("/api/productos/reservas") List<Skin> reservar(@RequestBody Reserva r);
}
