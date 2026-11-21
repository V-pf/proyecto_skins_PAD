package cl.skinspad.reporte.soporte;
import org.springframework.cloud.openfeign.FeignClient;import org.springframework.web.bind.annotation.*;import java.util.List;import java.math.BigDecimal;
@FeignClient(name="soporteCatalogo",url="${CATALOGO_URL:http://localhost:8081}") public interface SoporteCatalogoClient {
 record Publicacion(Long id,String nombre,String vendedorId,String tipo,String desgaste,Boolean stattrak,BigDecimal floatValor,String grado,BigDecimal precio,String estado,boolean reservada){}
 record Orden(String referencia,String actor,String motivo){}
 @GetMapping("/api/internal/moderacion/publicaciones/{id}") Publicacion obtener(@PathVariable("id") Long id);
 @GetMapping("/api/internal/moderacion/publicaciones") List<Publicacion> listar();
 @PostMapping("/api/internal/moderacion/publicaciones/{id}/despublicar") void despublicar(@PathVariable("id")Long id,@RequestBody Orden orden);
}
