package cl.skinspad.pago.stats;

import cl.skinspad.seguridad.VerificadorJwt;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

@Service
@RequiredArgsConstructor
public class UsuarioJwtClient implements StatsUserClient {
    private final Remote remoto;
    private final VerificadorJwt jwt;

    public Identity validate(String token) {
        jwt.verificar(token);
        try {
            return remoto.validarBearer("Bearer " + token);
        } catch (feign.FeignException.Forbidden e) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN);
        }
    }

    public List<PublicUser> users() {
        return remoto.users();
    }

    @FeignClient(name="statsUsersJwt", url="${services.usuario-url:http://localhost:8087}")
    public interface Remote {
        @GetMapping("/api/auth/validar") Identity validarBearer(@RequestHeader("Authorization") String bearer);
        @GetMapping("/api/usuarios/publicos") List<PublicUser> users();
    }
}
