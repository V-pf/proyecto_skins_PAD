package cl.skinspad.pago.stats;

import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/stats")
@RequiredArgsConstructor
public class StatsController {
  private final StatsService service;
  @GetMapping
  public StatsDto get(@RequestHeader(value="Authorization", required=false) String auth,
      @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam(defaultValue="false") boolean compare,
      @RequestParam(required=false) Long categoria,
      @RequestParam(required=false) String metodoPago) {
    return service.get(auth, from, to, compare, categoria, metodoPago);
  }
}
