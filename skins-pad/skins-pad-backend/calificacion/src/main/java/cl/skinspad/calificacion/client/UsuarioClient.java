package cl.skinspad.calificacion.client;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
@FeignClient(name="usuarios", url="${services.usuario-url}")
public interface UsuarioClient {
    record Identidad(String id, String rol, boolean suspendido) {}
    default Identidad validar(String token) { return validarBearer("Bearer " + token); }
  @GetMapping("/api/auth/validar") Identidad validarBearer(@RequestHeader("Authorization") String bearer);
}
