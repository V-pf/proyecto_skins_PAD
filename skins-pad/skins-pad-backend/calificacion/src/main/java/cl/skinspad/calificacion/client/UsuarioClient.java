package cl.skinspad.calificacion.client;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
@FeignClient(name="usuarios", url="${services.usuario-url}")
public interface UsuarioClient {
    record Identidad(String id, String rol, boolean suspendido) {}
    @GetMapping("/api/auth/validar") Identidad validar(@RequestParam("token") String token);
}
