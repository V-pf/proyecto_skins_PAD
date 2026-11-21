package cl.skinspad.steam.controller;

import cl.skinspad.steam.client.InventarioAuth;
import cl.skinspad.steam.client.SteamClient;
import cl.skinspad.steam.service.SteamVinculacionService;

import cl.skinspad.steam.config.ClienteHttp;
import cl.skinspad.steam.config.InternalApiKey;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
public class SteamController {
  private final SteamClient steam;
  private final SteamVinculacionService vinculacion;
  private final InventarioAuth auth;
  private final RestClient inventario;
  private final String clave;
  private final String frontend;

  public SteamController(SteamClient steam, SteamVinculacionService vinculacion, InventarioAuth auth,
      InternalApiKey clave,
      @Value("${INVENTARIO_SERVICE_URL:http://127.0.0.1:8082}") String inventarioUrl,
      @Value("${FRONTEND_URL:http://localhost:5173}") String frontend) {
    this.steam = steam;
    this.vinculacion = vinculacion;
    this.auth = auth;
    this.clave = clave.value();
    this.inventario = ClienteHttp.cliente(inventarioUrl, java.time.Duration.ofSeconds(135));
    this.frontend = frontend;
  }

  public record Vinculo(String steamId) {}

  @PostMapping("/api/steam/vincular")
  public Map<String, String> vincular(
      @RequestHeader(value = "Authorization", required = false) String token) {
    return Map.of("url", vinculacion.iniciar(auth.validar(token).id()));
  }

  @GetMapping("/api/steam/callback")
  public ResponseEntity<Void> callback(@RequestParam Map<String, String> params) {
    String resultado;
    try {
      String usuarioId = vinculacion.confirmar(params);
      try {
        inventario.post().uri("/interno/inventario/{usuarioId}/sincronizar", usuarioId)
            .header("X-Skins-Internal", clave).retrieve().toBodilessEntity();
        resultado = "vinculado";
      } catch (RuntimeException e) {
        resultado = "sincronizacion-pendiente";
      }
    } catch (RuntimeException e) {
      resultado = "error";
    }
    return ResponseEntity.status(HttpStatus.SEE_OTHER)
        .location(UriComponentsBuilder.fromUriString(frontend).path("/cliente/inventario")
            .queryParam("steam", resultado).build().toUri())
        .build();
  }

  @GetMapping("/interno/steam/usuarios/{usuarioId}/vinculo")
  public Vinculo vinculo(@PathVariable String usuarioId) {
    return new Vinculo(vinculacion.steamId(usuarioId));
  }

  @GetMapping("/interno/steam/usuarios/{usuarioId}/inventario")
  public List<SteamClient.ItemSteam> inventario(@PathVariable String usuarioId) {
    String steamId = vinculacion.steamId(usuarioId);
    if (steamId == null) {
      throw new IllegalStateException("Vincula tu cuenta de Steam para importar el inventario.");
    }
    return steam.inventario(steamId);
  }
}
