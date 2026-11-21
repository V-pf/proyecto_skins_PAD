package cl.skinspad.pago.mercado;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mercado/precios")
@RequiredArgsConstructor
public class PrecioMercadoController {
  private final PrecioMercadoService service;

  @GetMapping
  public List<PrecioMercadoService.Punto> consultar(
      @RequestParam String nombre, @RequestParam(defaultValue = "30") int dias) {
    return service.consultar(nombre, dias);
  }
}
