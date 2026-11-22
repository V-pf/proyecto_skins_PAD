package cl.skinspad.inventario.client;

import cl.skinspad.inventario.domain.ItemInventario;

import cl.skinspad.inventario.config.ClienteHttp;
import cl.skinspad.inventario.config.InternalApiKey;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class ServiciosInternos {
  private final RestClient steam;
  private final RestClient catalogo;
  private final String clave;

  public ServiciosInternos(
      @Value("${STEAM_SERVICE_URL:http://127.0.0.1:8088}") String steamUrl,
      @Value("${CATALOGO_URL:http://127.0.0.1:8081}") String catalogoUrl,
      InternalApiKey clave) {
    steam = ClienteHttp.cliente(steamUrl, Duration.ofSeconds(120));
    catalogo = ClienteHttp.cliente(catalogoUrl);
    this.clave = clave.value();
  }

  public record Vinculo(String steamId) {}
  public record Publicacion(Long itemId, Long productoId, String estado, boolean libre) {}
  public record Disponibilidad(Set<Long> disponibles) {}

  public String steamId(String usuarioId) {
    return recibir(() -> steam.get().uri("/interno/steam/usuarios/{id}/vinculo", usuarioId)
        .header("X-Skins-Internal", clave).retrieve().body(Vinculo.class)).steamId();
  }

  public List<ItemInventario> inventarioSteam(String usuarioId) {
    return List.of(recibir(() -> steam.get().uri("/interno/steam/usuarios/{id}/inventario", usuarioId)
        .header("X-Skins-Internal", clave).retrieve().body(ItemInventario[].class)));
  }

  public List<Publicacion> estados(String usuarioId) {
    return List.of(recibir(() -> catalogo.get().uri("/interno/publicaciones/{id}", usuarioId)
        .header("X-Skins-Internal", clave).retrieve().body(Publicacion[].class)));
  }

  public void actualizarDisponibilidad(String usuarioId, Set<Long> disponibles) {
    catalogo.put().uri("/interno/publicaciones/{id}/disponibilidad", usuarioId)
        .header("X-Skins-Internal", clave).body(new Disponibilidad(disponibles))
        .retrieve().toBodilessEntity();
  }

  private <T> T recibir(Supplier<T> peticion) {
    try {
      T respuesta = peticion.get();
      if (respuesta == null) throw new IllegalStateException("El servicio dependiente no devolvió datos.");
      return respuesta;
    } catch (RestClientResponseException e) {
      if (e.getStatusCode().value() == 409) {
        var error = e.getResponseBodyAs(JsonNode.class);
        if (error != null && error.path("message").isTextual())
          throw new IllegalStateException(error.path("message").asText());
      }
      throw e;
    }
  }
}
