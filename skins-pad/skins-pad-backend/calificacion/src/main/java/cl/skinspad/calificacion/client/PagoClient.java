package cl.skinspad.calificacion.client;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
@FeignClient(name="pagos", url="${services.pago-url}")
public interface PagoClient {
    record Operacion(String compradorId, String vendedorId, String estado) {}
    record Vista(Operacion operacion) {}
    @GetMapping("/api/operaciones/{id}") Vista obtener(@PathVariable("id") Long id, @RequestHeader("Authorization") String auth);
}
