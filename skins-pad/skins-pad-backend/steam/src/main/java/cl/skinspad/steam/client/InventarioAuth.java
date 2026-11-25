package cl.skinspad.steam.client;

import cl.skinspad.steam.config.ClienteHttp;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;
import org.springframework.web.server.ResponseStatusException;

@Component
public class InventarioAuth {
  public record Cuenta(
      String id, String username, String rol, boolean suspendido, boolean verificado) {}

  private final RestClient cliente;

  public InventarioAuth(@Value("${USUARIO_URL:http://localhost:8087}") String url) {
    cliente = ClienteHttp.cliente(url);
  }

  public Cuenta validar(String auth) {
    if (auth == null || !auth.startsWith("Bearer "))
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    Cuenta cuenta;
    try {
      cuenta =
          cliente
              .get()
              .uri("/api/auth/validar").header("Authorization", auth)
              .retrieve()
              .body(Cuenta.class);
    } catch (RestClientResponseException e) {
      throw new ResponseStatusException(
          e.getStatusCode().is5xxServerError()
              ? HttpStatus.SERVICE_UNAVAILABLE
              : HttpStatus.UNAUTHORIZED);
    }
    if (cuenta == null || cuenta.suspendido() || !"cliente".equals(cuenta.rol()))
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    return cuenta;
  }
}
