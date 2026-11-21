package cl.skinspad.reporte.soporte;
import java.util.*;
import org.springframework.cloud.openfeign.*;
import org.springframework.web.bind.annotation.*;
import static cl.skinspad.reporte.soporte.SoporteDtos.*;
@FeignClient(name="consultaTickets",url="${TICKET_URL:http://localhost:8085}")
public interface TicketConsultas {
 @GetMapping("/api/admin/tickets") Pagina bandeja(@RequestHeader("Authorization") String auth,@RequestParam Map<String,String> filtros);
 @GetMapping("/api/admin/soporte/stats") SoporteStats.Resumen stats(@RequestHeader("Authorization") String auth);
 @GetMapping("/api/soporte/notificaciones") List<Aviso> avisos(@RequestHeader("Authorization") String auth);
}
