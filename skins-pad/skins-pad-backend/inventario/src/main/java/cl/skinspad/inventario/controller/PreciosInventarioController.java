package cl.skinspad.inventario.controller;

import cl.skinspad.inventario.client.InventarioAuth;
import cl.skinspad.inventario.service.InventarioService;

import cl.skinspad.inventario.config.ClienteHttp;
import java.time.*;
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

  public record Serie(boolean demo, String moneda, String criterio, List<Punto> puntos) {}

  @GetMapping("/{id}/precios")
  public Serie precios(
      @PathVariable Long id,
      @RequestParam(defaultValue = "30") int dias,
      @RequestHeader(value = "Authorization", required = false) String token) {
    var cuenta = auth.validar(token);
    var item = inventario.itemDe(cuenta.id(), id);
    if (!Set.of(7, 30, 90).contains(dias))
      throw new IllegalArgumentException("Selecciona 7, 30 o 90 días.");
    if (item.isDemo()) {
      var puntos = new ArrayList<Punto>();
      int base = 20000 + Math.floorMod(item.getNombre().hashCode(), 70000);
      for (int n = dias - 1; n >= 0; n--) {
        LocalDate fecha = LocalDate.now(ZoneId.of("America/Santiago")).minusDays(n);
        long variacion = Math.floorMod(fecha.toEpochDay() * 37 + item.getNombre().hashCode(), 3500);
        puntos.add(new Punto(fecha, base + variacion, 1 + Math.floorMod(fecha.toEpochDay(), 5)));
      }
      return new Serie(true, "CLP", item.getNombre(), puntos);
    }
    Punto[] puntos =
        ClienteHttp.cliente(pagos)
            .get()
            .uri(
                b ->
                    b.path("/api/mercado/precios")
                        .queryParam("nombre", item.getNombre())
                        .queryParam("dias", dias)
                        .build())
            .retrieve()
            .body(Punto[].class);
    return new Serie(
        false, "CLP", item.getNombre(), puntos == null ? List.of() : Arrays.asList(puntos));
  }
}
