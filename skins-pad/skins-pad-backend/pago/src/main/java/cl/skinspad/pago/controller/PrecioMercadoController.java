package cl.skinspad.pago.controller;

import cl.skinspad.pago.service.HistorialPreciosService;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mercado/precios")
@RequiredArgsConstructor
public class PrecioMercadoController {
  private final HistorialPreciosService service;

  @GetMapping
  public List<HistorialPreciosService.Punto> consultar(
      @RequestParam String nombre,
      @RequestParam(defaultValue = "30") int dias,
      @RequestParam(required = false) String desgaste,
      @RequestParam(required = false) Boolean stattrak) {
    return service.consultar(nombre, dias, desgaste, stattrak);
  }
}
