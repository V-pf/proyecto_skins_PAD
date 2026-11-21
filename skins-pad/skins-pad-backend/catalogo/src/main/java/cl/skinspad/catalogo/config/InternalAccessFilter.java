package cl.skinspad.catalogo.config;
import jakarta.servlet.*;import jakarta.servlet.http.*;import java.io.IOException;import java.nio.charset.StandardCharsets;import java.security.MessageDigest;
@org.springframework.stereotype.Component
public class InternalAccessFilter extends org.springframework.web.filter.OncePerRequestFilter {
 private final String key;
 public InternalAccessFilter(InternalApiKey credential){this.key=credential.value();}
 protected void doFilterInternal(HttpServletRequest r,HttpServletResponse s,FilterChain c)throws ServletException,IOException{
   boolean internal=r.getServletPath().startsWith("/interno/") || r.getRequestURI().contains("/saldo/") || r.getRequestURI().equals("/api/productos/reservas");
   if(internal){String provided=r.getHeader("X-Skins-Internal");if(key.isBlank()||provided==null||!MessageDigest.isEqual(key.getBytes(StandardCharsets.UTF_8),provided.getBytes(StandardCharsets.UTF_8))){s.sendError(403);return;}}
   c.doFilter(r,s);
 }
}
