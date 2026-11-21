package cl.skinspad.reporte.config;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import feign.FeignException;
import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> notFound(RuntimeException ex) {
        return response(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> badRequest(RuntimeException ex) {
        return response(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> conflict(RuntimeException ex) {
        return response(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<Map<String, Object>> upstream(FeignException ex) {
        if (ex.status() == 401 || ex.status() == 403) {
            return response(HttpStatus.BAD_GATEWAY, "El servicio dependiente rechazó la credencial interna. Reinicia usuario, catálogo y pagos con la misma configuración.");
        }
        if(ex.status()==409)return response(HttpStatus.CONFLICT,"La operación no pudo completarse. Comprueba que la publicación siga disponible.");
        if (ex.status() == 404) {
            return response(HttpStatus.NOT_FOUND, "El usuario o recurso solicitado no existe en el servicio dependiente");
        }
        return response(HttpStatus.BAD_GATEWAY,
            "No fue posible comunicarse con el microservicio dependiente");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validation(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream().map(e -> e.getField() + ": " + e.getDefaultMessage()).findFirst().orElse("Datos inválidos");
        return response(HttpStatus.BAD_REQUEST, msg);
    }

    private ResponseEntity<Map<String, Object>> response(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("timestamp", Instant.now().toString(), "status", status.value(), "error", status.getReasonPhrase(), "message", message == null ? "Error" : message));
    }
}
