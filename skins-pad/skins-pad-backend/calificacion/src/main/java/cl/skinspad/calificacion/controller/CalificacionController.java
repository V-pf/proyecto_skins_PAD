package cl.skinspad.calificacion.controller;
import cl.skinspad.calificacion.service.CalificacionService;
import cl.skinspad.calificacion.domain.Resena;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.dao.DataIntegrityViolationException;
import java.util.Map;
@RestController @RequestMapping("/api/calificaciones") @RequiredArgsConstructor
public class CalificacionController {
    private final CalificacionService service;
    public record Nueva(@NotNull @Positive Long operacionId,
        @NotNull @DecimalMin("1.0") @DecimalMax("5.0") @Digits(integer=1,fraction=2) BigDecimal puntaje,
        @NotBlank @Size(max=1000) String comentario) {}
    @GetMapping("/vendedor/{id}")
    public CalificacionService.Reputacion consultar(@PathVariable String id) { return service.consultar(id); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public Resena crear(@RequestHeader(value="Authorization",required=false) String auth, @Valid @RequestBody Nueva nueva) {
        return service.crear(auth, nueva.operacionId(), nueva.puntaje(), nueva.comentario());
    }
    @ExceptionHandler(DataIntegrityViolationException.class) @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String,String> duplicada() { return Map.of("message", "Esta compra ya tiene una reseña."); }
}
