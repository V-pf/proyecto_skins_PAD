package cl.skinspad.catalogo.controller;

import cl.skinspad.catalogo.client.InventarioAuth;
import cl.skinspad.catalogo.domain.Producto;
import cl.skinspad.catalogo.dto.DisponibilidadInventarioRequest;
import cl.skinspad.catalogo.dto.EstadoPublicacionInventario;
import cl.skinspad.catalogo.dto.PublicarDesdeInventarioRequest;
import cl.skinspad.catalogo.service.CatalogoService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class PublicacionInventarioController {
  private final InventarioAuth auth;
  private final CatalogoService catalogo;

  @GetMapping("/interno/publicaciones/{usuarioId}")
  public List<EstadoPublicacionInventario> estados(@PathVariable String usuarioId) {
    return catalogo.estadosDeInventario(usuarioId);
  }

  @PutMapping("/interno/publicaciones/{usuarioId}/disponibilidad")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void disponibilidad(@PathVariable String usuarioId, @Valid @RequestBody DisponibilidadInventarioRequest datos) {
    catalogo.actualizarDisponibilidadInventario(usuarioId, datos.disponibles());
  }

  @PostMapping("/api/inventario/publicaciones")
  @ResponseStatus(HttpStatus.CREATED)
  public Producto publicar(
      @RequestHeader(value = "Authorization", required = false) String token,
      @Valid @RequestBody PublicarDesdeInventarioRequest peticion) {
    return catalogo.publicarDesdeInventario(auth.validar(token), peticion.itemId(), peticion.precio());
  }

}
