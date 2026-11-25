package cl.skinspad.catalogo.config;
import jakarta.servlet.*;import jakarta.servlet.http.*;import java.io.IOException;import java.util.Map;
@org.springframework.stereotype.Component @lombok.RequiredArgsConstructor
public class CatalogOwnerFilter extends org.springframework.web.filter.OncePerRequestFilter {
 private final cl.skinspad.seguridad.VerificadorJwt jwt;
 private final cl.skinspad.catalogo.repository.ProductoRepository productos;
 @org.springframework.beans.factory.annotation.Value("${USUARIO_URL:http://localhost:8087}") private String url;
 protected void doFilterInternal(HttpServletRequest r,HttpServletResponse s,FilterChain c)throws ServletException,IOException{
  String p=r.getRequestURI();if((!p.startsWith("/api/productos")&&!p.startsWith("/api/categorias"))||r.getMethod().equals("GET")||r.getMethod().equals("OPTIONS")||p.equals("/api/productos/reservas")){c.doFilter(r,s);return;}
  String auth=r.getHeader("Authorization");if(auth==null||!auth.startsWith("Bearer ")){s.sendError(401);return;}
  Map u;try{jwt.verificar(auth.substring(7));u=org.springframework.web.client.RestClient.create(url).get().uri("/api/auth/validar").header("Authorization",auth).retrieve().body(Map.class);}catch(org.springframework.web.server.ResponseStatusException e){s.sendError(e.getStatusCode().value());return;}catch(org.springframework.web.client.HttpClientErrorException e){s.sendError(e.getStatusCode().value());return;}catch(org.springframework.web.client.RestClientException e){s.sendError(503);return;}
  if(u==null||Boolean.TRUE.equals(u.get("suspendido"))){s.sendError(403);return;}
  boolean admin="admin".equals(u.get("rol"));r.setAttribute("actor",u.get("id"));r.setAttribute("admin",admin);
  if(p.matches("/api/productos/[0-9]+(/.*)?")){
    var o=productos.findById(Long.valueOf(p.split("/")[3]));if(o.isEmpty()){s.sendError(404);return;}
    if(!admin&&(!o.get().getVendedorId().equals(u.get("id"))||p.endsWith("/estado"))){s.sendError(403);return;}
  }else if(!p.equals("/api/productos")&&!admin){s.sendError(403);return;}
  if(p.equals("/api/productos")&&(admin||!Boolean.TRUE.equals(u.get("verificado")))){s.sendError(403);return;}
  c.doFilter(r,s);
 }
}
