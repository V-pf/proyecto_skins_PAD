package cl.skinspad.pago.stats;

import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name="statsUsers", url="${services.usuario-url:http://localhost:8087}")
public interface StatsUserClient {
  record Identity(String id, String rol, boolean suspendido) {}
  record PublicUser(String id, String rol) {}
  @GetMapping("/api/auth/validar") Identity validate(@RequestParam("token") String token);
  @GetMapping("/api/usuarios/publicos") List<PublicUser> users();
}
