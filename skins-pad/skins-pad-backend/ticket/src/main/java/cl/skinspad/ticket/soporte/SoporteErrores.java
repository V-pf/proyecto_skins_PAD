package cl.skinspad.ticket.soporte;
import java.util.Map;import org.springframework.web.bind.annotation.*;import org.springframework.http.ResponseEntity;
@RestControllerAdvice @org.springframework.core.annotation.Order(0) public class SoporteErrores {
 @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class) public ResponseEntity<?> estado(org.springframework.web.server.ResponseStatusException e){return ResponseEntity.status(e.getStatusCode()).body(Map.of("message",e.getReason()==null?"Acceso no permitido":e.getReason()));}
 @ExceptionHandler({org.springframework.orm.ObjectOptimisticLockingFailureException.class,jakarta.persistence.OptimisticLockException.class,org.springframework.dao.PessimisticLockingFailureException.class}) public ResponseEntity<?> conflicto(Exception e){return ResponseEntity.status(409).body(Map.of("message","El caso cambió, recarga antes de continuar"));}
 @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class) public ResponseEntity<?> duplicado(Exception e){return ResponseEntity.status(409).body(Map.of("message","Ya existe una solicitud abierta o los datos cambiaron. Recarga antes de reintentar"));}
}
