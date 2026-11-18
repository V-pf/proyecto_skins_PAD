package cl.skinspad.carrito.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/** Cliente HTTP hacia el microservicio de catálogo. */
@FeignClient(
    name = "skins-pad-catalogo",
    url = "${services.catalogo-url:http://localhost:8081}"
)
public interface CatalogoClient {

    @GetMapping("/api/productos/{id}")
    ProductoRemoto buscarProducto(@PathVariable("id") Long id);
}
