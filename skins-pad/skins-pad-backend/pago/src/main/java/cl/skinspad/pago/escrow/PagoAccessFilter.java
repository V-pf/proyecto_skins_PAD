package cl.skinspad.pago.escrow;
import jakarta.servlet.*;import jakarta.servlet.http.*;import java.io.IOException;
import lombok.RequiredArgsConstructor;import org.springframework.stereotype.Component;import org.springframework.web.filter.OncePerRequestFilter;
import cl.skinspad.pago.stats.StatsUserClient;import cl.skinspad.pago.repository.PagoRepository;
@Component @RequiredArgsConstructor
public class PagoAccessFilter extends OncePerRequestFilter {
 private final StatsUserClient users;private final PagoRepository pagos;
 @Override protected void doFilterInternal(HttpServletRequest r,HttpServletResponse response,FilterChain chain)throws ServletException,IOException{
   String path=r.getRequestURI();if(!path.startsWith("/api/pagos")){chain.doFilter(r,response);return;}
   String auth=r.getHeader("Authorization");if(auth==null||!auth.startsWith("Bearer ")){response.sendError(401);return;}
   StatsUserClient.Identity u;try{u=users.validate(auth.substring(7));}catch(feign.FeignException e){response.sendError(e.status()>=500?503:401);return;}
   if(u.suspendido()){response.sendError(403);return;}boolean admin="admin".equals(u.rol()),get=r.getMethod().equals("GET"),ok=false;
   if(path.equals("/api/pagos/checkout"))ok=!admin && r.getMethod().equals("POST");
   else if(path.equals("/api/pagos/mis-ventas"))ok=!admin&&get;
   else if(path.equals("/api/pagos"))ok=admin&&get;
   else if(path.startsWith("/api/pagos/usuario/"))ok=get&&(admin||path.equals("/api/pagos/usuario/"+u.id()));
   else if(path.matches("/api/pagos/[0-9]+(/.*)?")){
     var pago=pagos.findById(Long.valueOf(path.split("/")[3]));
     if(pago.isEmpty()){response.sendError(404);return;}
     ok=(admin||u.id().equals(pago.get().getUsuarioId()))&&(get||path.endsWith("/comprobante")||admin&&path.endsWith("/comprobante/verificar"));
   }else if(path.startsWith("/api/pagos/pedido/")){String ref=path.substring(18).replaceFirst("/estado$","");var pago=pagos.findByPedidoId(ref);ok=get&&pago.isPresent()&&(admin||u.id().equals(pago.get().getUsuarioId()));}
   if(!ok){response.sendError(403);return;}
   chain.doFilter(new HttpServletRequestWrapper(r){@Override public String getHeader(String name){return name.equalsIgnoreCase("X-Skins-Usuario")?u.id():super.getHeader(name);}},response);
 }
}
