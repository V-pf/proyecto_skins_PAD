package cl.skinspad.ticket.soporte;
import org.springframework.cloud.openfeign.FeignClient;import org.springframework.web.bind.annotation.*;import java.util.List;import java.math.BigDecimal;
@FeignClient(name="soporteCatalogo",url="${CATALOGO_URL:http://localhost:8081}") public interface SoporteCatalogoClient {
 record Publicacion(Long id,String nombre,String vendedorId,String tipo,String desgaste,Boolean stattrak,BigDecimal floatValor,String grado,BigDecimal precio,String estado,boolean reservada){}
 @GetMapping("/api/internal/moderacion/publicaciones/{id}") Publicacion obtener(@PathVariable("id") Long id);
}
