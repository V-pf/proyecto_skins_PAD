package cl.skinspad.catalogo.client;

import cl.skinspad.catalogo.config.ClienteHttp;
import cl.skinspad.catalogo.config.InternalApiKey;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class InventarioClient {
  private final RestClient cliente;
  private final String clave;

  public InventarioClient(
      @Value("${INVENTARIO_SERVICE_URL:http://127.0.0.1:8082}") String url,
      InternalApiKey clave) {
    cliente = ClienteHttp.cliente(url, Duration.ofSeconds(130));
    this.clave = clave.value();
  }

  public record Item(Long id, String nombre, String tipo, String grado, String desgaste,
      boolean stattrak, String imagen, boolean demo, Long productoOrigenId) {}

  public record Traslado(Long productoOrigenId, Long inventarioId, String nombre, String tipo,
      String grado, String desgaste, boolean stattrak, BigDecimal floatValor, String imagen) {}

  public Long guardarTraslado(String usuarioId, Traslado traslado) {
    Item item = cliente.put().uri("/interno/inventario/{usuario}/traslados/{producto}",
            usuarioId, traslado.productoOrigenId())
        .header("X-Skins-Internal", clave).body(traslado).retrieve().body(Item.class);
    if (item == null || item.id() == null
        || (!traslado.productoOrigenId().equals(item.productoOrigenId())
            && !item.id().equals(traslado.inventarioId()))) {
      throw new IllegalStateException("Inventario no confirmó el traslado del producto.");
    }
    return item.id();
  }

  public Item verificarDisponible(String usuarioId, Long itemId) {
    try {
      Item item = cliente.get().uri("/interno/inventario/{usuario}/items/{id}/disponible", usuarioId, itemId)
          .header("X-Skins-Internal", clave).retrieve().body(Item.class);
      if (item == null || !itemId.equals(item.id()) || (item.tipo() == null && item.productoOrigenId() == null)) {
        throw new IllegalStateException("El servicio de inventario no confirmó la disponibilidad.");
      }
      return item;
    } catch (RestClientResponseException e) {
      if (e.getStatusCode().value() == 409 || e.getStatusCode().value() == 400) {
        var error = e.getResponseBodyAs(JsonNode.class);
        if (error != null && error.path("message").isTextual()) {
          if (e.getStatusCode().value() == 400) {
            throw new IllegalArgumentException(error.path("message").asText());
          }
          throw new IllegalStateException(error.path("message").asText());
        }
      }
      throw e;
    }
  }
}
