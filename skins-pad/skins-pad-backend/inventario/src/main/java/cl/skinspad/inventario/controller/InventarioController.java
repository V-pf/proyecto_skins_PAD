package cl.skinspad.inventario.controller;

import cl.skinspad.inventario.client.InventarioAuth;
import cl.skinspad.inventario.domain.ItemInventario;
import cl.skinspad.inventario.service.InventarioService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class InventarioController {
  private final InventarioAuth auth;
  private final InventarioService inventario;

  public record Traslado(Long inventarioId, @NotBlank @Size(max = 160) String nombre, String tipo, String grado,
      String desgaste, boolean stattrak, BigDecimal floatValor, String imagen) {}

  @PutMapping("/interno/inventario/{usuarioId}/traslados/{productoId}")
  public ItemInventario trasladar(@PathVariable String usuarioId, @PathVariable Long productoId,
      @Valid @RequestBody Traslado datos) {
    return inventario.guardarTraslado(usuarioId, productoId, datos.inventarioId(), datos.nombre(),
        datos.tipo(), datos.grado(), datos.desgaste(), datos.stattrak(), datos.floatValor(), datos.imagen());
  }

  @GetMapping("/api/inventario")
  public InventarioService.Inventario listar(
      @RequestHeader(value = "Authorization", required = false) String token) {
    return inventario.consultar(auth.validar(token).id());
  }

  @PostMapping("/api/inventario/sincronizar")
  public InventarioService.Inventario sincronizar(
      @RequestHeader(value = "Authorization", required = false) String token) {
    String id = auth.validar(token).id();
    inventario.sincronizar(id);
    return inventario.consultar(id);
  }

  @PostMapping("/interno/inventario/{usuarioId}/sincronizar")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void sincronizarVinculo(@PathVariable String usuarioId) {
    inventario.sincronizar(usuarioId);
  }

  @GetMapping("/interno/inventario/{usuarioId}/items/{itemId}/disponible")
  public ItemInventario disponible(@PathVariable String usuarioId, @PathVariable Long itemId) {
    return inventario.verificarDisponible(usuarioId, itemId);
  }
}
