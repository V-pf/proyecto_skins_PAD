package cl.skinspad.catalogo.inventario;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequiredArgsConstructor
public class InventarioController {
  private final InventarioAuth auth;
  private final InventarioService inventario;
  private final SteamVinculacionService steam;

  @Value("${FRONTEND_URL:http://localhost:5173}")
  private String frontend;

  public record Publicar(
      @NotNull @Positive Long itemId,
      @NotNull @DecimalMin("1") @DecimalMax("50000000") @Digits(integer = 8, fraction = 0)
          BigDecimal precio) {}

  @GetMapping("/api/inventario")
  public InventarioService.Inventario listar(
      @RequestHeader(value = "Authorization", required = false) String token) {
    return inventario.consultar(auth.validar(token).id());
  }

  @PostMapping("/api/inventario/sincronizar")
  public InventarioService.Inventario sincronizar(
      @RequestHeader(value = "Authorization", required = false) String token) {
    var cuenta = auth.validar(token);
    inventario.sincronizar(cuenta.id());
    return inventario.consultar(cuenta.id());
  }

  @PostMapping("/api/inventario/publicaciones")
  @ResponseStatus(HttpStatus.CREATED)
  public cl.skinspad.catalogo.domain.Producto publicar(
      @RequestHeader(value = "Authorization", required = false) String token,
      @Valid @RequestBody Publicar peticion) {
    return inventario.publicar(auth.validar(token), peticion.itemId(), peticion.precio());
  }

  @PostMapping("/api/steam/vincular")
  public Map<String, String> vincular(
      @RequestHeader(value = "Authorization", required = false) String token) {
    return Map.of("url", steam.iniciar(auth.validar(token).id()));
  }

  @GetMapping("/api/steam/callback")
  public ResponseEntity<Void> callback(@RequestParam Map<String, String> params) {
    String resultado;
    try {
      String usuarioId = steam.confirmar(params);
      try {
        inventario.sincronizar(usuarioId);
        resultado = "vinculado";
      } catch (RuntimeException e) {
        resultado = "sincronizacion-pendiente";
      }
    } catch (RuntimeException e) {
      resultado = "error";
    }
    return ResponseEntity.status(HttpStatus.SEE_OTHER)
        .location(
            UriComponentsBuilder.fromUriString(frontend)
                .path("/cliente/inventario")
                .queryParam("steam", resultado)
                .build()
                .toUri())
        .build();
  }
}
