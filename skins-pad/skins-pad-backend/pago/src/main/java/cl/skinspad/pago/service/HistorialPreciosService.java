package cl.skinspad.pago.service;

import cl.skinspad.pago.escrow.*;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class HistorialPreciosService {
  private final OperacionRepository operaciones;

  @Value("${CATALOGO_URL:http://localhost:8081}")
  private String catalogo;

  public record Producto(Long id, String nombre, Boolean demo, String desgaste, Boolean stattrak) {}

  public record Punto(LocalDate fecha, double precioMedio, long ventas) {}

  static String nombreMercado(Producto p) {
    String nombre = p.nombre();
    if (Boolean.TRUE.equals(p.stattrak()) && !nombre.contains("StatTrak"))
      nombre = "StatTrak™ " + nombre;
    String exterior =
        switch (p.desgaste() == null ? "na" : p.desgaste()) {
          case "fn" -> "Factory New";
          case "mw" -> "Minimal Wear";
          case "ft" -> "Field-Tested";
          case "ww" -> "Well-Worn";
          case "bs" -> "Battle-Scarred";
          default -> "";
        };
    if (!exterior.isEmpty() && !nombre.endsWith("(" + exterior + ")"))
      nombre += " (" + exterior + ")";
    return nombre;
  }

  public List<Punto> consultar(String nombre, int dias) {
    if (!Set.of(7, 30, 90).contains(dias) || nombre.isBlank() || nombre.length() > 255)
      throw new IllegalArgumentException("Consulta de precios inválida.");
    var productos =
        RestClient.create(catalogo).get().uri("/api/productos").retrieve().body(Producto[].class);
    if (productos == null) return List.of();
    Set<Long> ids =
        Arrays.stream(productos)
            .filter(p -> nombre.equals(nombreMercado(p)) && !Boolean.TRUE.equals(p.demo()))
            .map(Producto::id)
            .collect(Collectors.toSet());
    ZoneId zona = ZoneId.of("America/Santiago");
    Instant desde = LocalDate.now(zona).minusDays(dias - 1).atStartOfDay(zona).toInstant();
    var grupos =
        operaciones
            .findByEstadoAndCompletadaGreaterThanEqual(EstadoOperacion.COMPLETADA, desde)
            .stream()
            .filter(
                o -> ids.contains(o.getProductoId()) && !o.getCompletada().isAfter(Instant.now()))
            .collect(
                Collectors.groupingBy(
                    o -> o.getCompletada().atZone(zona).toLocalDate(),
                    TreeMap::new,
                    Collectors.summarizingLong(Operacion::getPrecio)));
    return grupos.entrySet().stream()
        .map(e -> new Punto(e.getKey(), e.getValue().getAverage(), e.getValue().getCount()))
        .toList();
  }
}
