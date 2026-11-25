package cl.skinspad.usuario.config;

import cl.skinspad.usuario.domain.Usuario;
import cl.skinspad.usuario.service.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
public class WalletOwnerFilter extends OncePerRequestFilter {
    private final TokensJwt jwt;
    private final UsuarioService usuarios;
    private final InternalApiKey interna;

    protected void doFilterInternal(HttpServletRequest r, HttpServletResponse s, FilterChain chain)
            throws IOException, ServletException {
        String p = r.getRequestURI(), m = r.getMethod();
        boolean lectura = m.equals("GET");
        if (!p.startsWith("/api/usuarios") || m.equals("OPTIONS") || p.contains("/saldo/")
                || lectura && p.equals("/api/usuarios/publicos")) {
            chain.doFilter(r, s); return;
        }
        String key = r.getHeader("X-Skins-Internal");
        if (lectura && p.matches("/api/usuarios/[^/]+") && key != null && MessageDigest.isEqual(
                key.getBytes(StandardCharsets.UTF_8), interna.value().getBytes(StandardCharsets.UTF_8))) {
            chain.doFilter(r, s); return;
        }
        Usuario u;
        try {
            u = usuarios.usuarioDeSesion(jwt.sesion(r.getHeader("Authorization")));
        } catch (ResponseStatusException | IllegalArgumentException | jakarta.persistence.EntityNotFoundException e) {
            s.sendError(401); return;
        }
        String propio = "/api/usuarios/" + u.getId();
        boolean admin = u.getRol().name().equals("admin");
        boolean permitido = admin ? !p.endsWith("/perfil") && !p.endsWith("/retiros")
            : lectura && p.equals(propio) || p.equals(propio + "/perfil") || p.equals(propio + "/retiros");
        if (u.isSuspendido() || !permitido) { s.sendError(403); return; }
        chain.doFilter(r, s);
    }
}
