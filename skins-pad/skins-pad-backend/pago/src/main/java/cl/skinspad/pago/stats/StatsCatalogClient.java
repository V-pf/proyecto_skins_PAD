package cl.skinspad.pago.stats;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name="statsCatalog", url="${CATALOGO_URL:http://localhost:8081}")
public interface StatsCatalogClient {
  record Category(Long id, String nombre) {}
  record Picture(String urlImagen, boolean esPrincipal) {}
  record Product(Long id, String nombre, String vendedorId, Category categoria, String tipo,
      String estado, LocalDateTime fechaCreacion, String motivoRechazo, String desgaste,
      String grado, Boolean stattrak, BigDecimal floatValor, List<Picture> imagenes) {}
  @GetMapping("/api/productos") List<Product> products();
  @GetMapping("/api/categorias") List<Category> categories();
}
