package cl.skinspad.inventario.client;

import cl.skinspad.inventario.config.ClienteHttp;
import cl.skinspad.inventario.config.InternalApiKey;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ServiciosInternos {
  private final RestClient catalogo;
  private final String clave;

  public ServiciosInternos(
      @Value("${CATALOGO_URL:http://127.0.0.1:8081}") String catalogoUrl,
      InternalApiKey clave) {
    catalogo = ClienteHttp.cliente(catalogoUrl);
    this.clave = clave.value();
  }

  public record Publicacion(Long itemId, Long productoId, String estado, boolean libre) {}

  public List<Publicacion> estados(String usuarioId) {
    var respuesta = catalogo.get().uri("/interno/publicaciones/{id}", usuarioId)
        .header("X-Skins-Internal", clave).retrieve().body(Publicacion[].class);
    if (respuesta == null) throw new IllegalStateException("El catálogo no devolvió los estados de las publicaciones.");
    return List.of(respuesta);
  }
}
