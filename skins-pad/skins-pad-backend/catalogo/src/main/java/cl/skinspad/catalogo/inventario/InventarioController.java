package cl.skinspad.catalogo.inventario;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class InventarioController {
  private final InventarioAuth auth;
  private final InventarioService inventario;

  public record Publicar(
      @NotNull @Positive Long itemId,
      @NotNull @DecimalMin("1") @DecimalMax("50000000") @Digits(integer = 8, fraction = 0)
          BigDecimal precio) {}

  public record Disponibilidad(@NotNull Set<Long> disponibles) {}

  @GetMapping("/interno/publicaciones/{usuarioId}")
  public List<InventarioService.Publicacion> estados(@PathVariable String usuarioId) {
    return inventario.estados(usuarioId);
  }

  @PutMapping("/interno/publicaciones/{usuarioId}/disponibilidad")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void disponibilidad(@PathVariable String usuarioId, @Valid @RequestBody Disponibilidad datos) {
    inventario.actualizarDisponibilidad(usuarioId, datos.disponibles());
  }

  @PostMapping("/api/inventario/publicaciones")
  @ResponseStatus(HttpStatus.CREATED)
  public cl.skinspad.catalogo.domain.Producto publicar(
      @RequestHeader(value = "Authorization", required = false) String token,
      @Valid @RequestBody Publicar peticion) {
    return inventario.publicar(auth.validar(token), peticion.itemId(), peticion.precio());
  }

}
