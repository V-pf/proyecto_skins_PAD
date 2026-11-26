package cl.skinspad.inventario.controller;

import cl.skinspad.inventario.client.InventarioAuth;
import cl.skinspad.inventario.service.InventarioService;

import cl.skinspad.inventario.config.ClienteHttp;
import java.time.LocalDate;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventario")
@RequiredArgsConstructor
public class PreciosInventarioController {
  private final InventarioAuth auth;
  private final InventarioService inventario;

  @Value("${PAGO_URL:http://localhost:8084}")
  private String pagos;

  public record Punto(LocalDate fecha, double precioMedio, long ventas) {}

  public record Serie(String moneda, String criterio, List<Punto> puntos) {}

  @GetMapping("/{id}/precios")
  public Serie precios(
      @PathVariable Long id,
      @RequestParam(defaultValue = "30") int dias,
      @RequestHeader(value = "Authorization", required = false) String token) {
    var cuenta = auth.validar(token);
    var item = inventario.itemDe(cuenta.id(), id);
    if (!Set.of(7, 30, 90).contains(dias))
      throw new IllegalArgumentException("Selecciona 7, 30 o 90 días.");
    Punto[] puntos =
        ClienteHttp.cliente(pagos)
            .get()
            .uri(
                b ->
                    b.path("/api/mercado/precios")
                        .queryParam("nombre", item.getNombre())
                        .queryParam("dias", dias)
                        .queryParamIfPresent("desgaste", Optional.ofNullable(item.getDesgaste()))
                        .queryParam("stattrak", item.isStattrak())
                        .build())
            .retrieve()
            .body(Punto[].class);
    return new Serie("CLP", item.getNombre(), puntos == null ? List.of() : Arrays.asList(puntos));
  }
}
