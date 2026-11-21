package cl.skinspad.ticket.soporte;
import cl.skinspad.ticket.stats.StatsUserClient;import lombok.RequiredArgsConstructor;import org.springframework.stereotype.Component;import org.springframework.web.server.ResponseStatusException;import org.springframework.http.HttpStatus;
@Component @RequiredArgsConstructor public class SoporteIdentidad {
 private final StatsUserClient usuarios;
 public record Actor(String id,boolean admin){}
 public Actor validar(String auth,boolean admin){if(auth==null||!auth.startsWith("Bearer "))throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);StatsUserClient.Identity u;try{u=usuarios.validate(auth.substring(7));}catch(feign.FeignException e){throw new ResponseStatusException(e.status()<0||e.status()>=500?HttpStatus.SERVICE_UNAVAILABLE:HttpStatus.UNAUTHORIZED);}if(u==null||u.suspendido()||!java.util.Set.of("admin","cliente").contains(u.rol()))throw new ResponseStatusException(HttpStatus.FORBIDDEN);var a=new Actor(u.id(),"admin".equals(u.rol()));if(admin&&!a.admin())throw new ResponseStatusException(HttpStatus.FORBIDDEN);return a;}
 public static void propietario(String id,Actor a){if(!a.admin()&&!id.equals(a.id()))throw new ResponseStatusException(HttpStatus.FORBIDDEN);}
 public static void cliente(Actor a){if(a.admin())throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Usa la bandeja administrativa");}
}
