package cl.skinspad.carrito.config;

import cl.skinspad.carrito.repository.CarritoRepository;
import cl.skinspad.seguridad.VerificadorJwt;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;

@Component
public class CarritoAccessFilter extends OncePerRequestFilter {
    public record Actor(String id, String rol, boolean suspendido) {}
    private final CarritoRepository carritos;
    private final VerificadorJwt jwt;
    private final RestClient usuarios;

    public CarritoAccessFilter(CarritoRepository carritos, VerificadorJwt jwt,
            @Value("${USUARIO_URL:http://localhost:8087}") String url) {
        this.carritos = carritos;
        this.jwt = jwt;
        this.usuarios = RestClient.create(url);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest r, HttpServletResponse s, FilterChain chain)
            throws IOException, ServletException {
        String path = r.getRequestURI();
        if (!path.startsWith("/api/carritos/") || r.getMethod().equals("OPTIONS")) {
            chain.doFilter(r, s);
            return;
        }
        String bearer = r.getHeader("Authorization");
        if (bearer == null || !bearer.startsWith("Bearer ")) { s.sendError(401); return; }
        Actor actor;
        try {
            jwt.verificar(bearer.substring(7));
            actor = usuarios.get().uri("/api/auth/validar").header("Authorization", bearer).retrieve().body(Actor.class);
        } catch (ResponseStatusException e) {
            s.sendError(e.getStatusCode().value()); return;
        } catch (HttpClientErrorException e) {
            s.sendError(e.getStatusCode().value()); return;
        } catch (RestClientException e) {
            s.sendError(503); return;
        }
        if (actor == null || actor.suspendido() || !"cliente".equals(actor.rol())) { s.sendError(403); return; }
        boolean permitido = path.equals("/api/carritos/usuario/" + actor.id());
        if (path.matches("/api/carritos/[0-9]+(/.*)?")) {
            var carrito = carritos.findById(Long.valueOf(path.split("/")[3]));
            if (carrito.isEmpty()) { s.sendError(404); return; }
            permitido = actor.id().equals(carrito.get().getUsuarioId());
        }
        if (!permitido) { s.sendError(403); return; }
        chain.doFilter(r, s);
    }
}
