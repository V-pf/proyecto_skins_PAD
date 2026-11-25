package cl.skinspad.ticket.stats;

import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name="statsUsers", url="${services.usuario-url:http://localhost:8087}")
public interface StatsUserClient {
  record Identity(String id, String rol, boolean suspendido) {}
  record PublicUser(String id, String rol) {}
  default Identity validate(String token) { return validarBearer("Bearer " + token); }
  @GetMapping("/api/auth/validar") Identity validarBearer(@RequestHeader("Authorization") String bearer);
  @GetMapping("/api/usuarios/publicos") List<PublicUser> users();
}
