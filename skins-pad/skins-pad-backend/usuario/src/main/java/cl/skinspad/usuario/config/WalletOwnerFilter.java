package cl.skinspad.usuario.config;
import jakarta.servlet.*;import jakarta.servlet.http.*;import java.io.IOException;
@org.springframework.stereotype.Component @lombok.RequiredArgsConstructor
public class WalletOwnerFilter extends org.springframework.web.filter.OncePerRequestFilter {
 private final cl.skinspad.usuario.service.UsuarioService usuarios;
 protected void doFilterInternal(HttpServletRequest r,HttpServletResponse s,FilterChain c)throws ServletException,IOException {
  String p=r.getRequestURI();if(!p.matches("/api/usuarios/[^/]+/(retiros|perfil|estado)")){c.doFilter(r,s);return;}
  String auth=r.getHeader("Authorization");if(auth==null||!auth.startsWith("Bearer ")){s.sendError(401);return;}
  try{var u=usuarios.validarToken(auth.substring(7));if(u.isSuspendido() || (p.endsWith("/estado") ? !u.getRol().name().equals("admin") : u.getRol().name().equals("admin") || !p.startsWith("/api/usuarios/"+u.getId()+"/"))){s.sendError(403);return;}}catch(RuntimeException e){s.sendError(401);return;}
  c.doFilter(r,s);
 }
}
