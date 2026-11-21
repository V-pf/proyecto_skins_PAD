package cl.skinspad.ticket.soporte;
import java.util.*;import java.time.*;import static cl.skinspad.ticket.soporte.EstadosSoporte.*;
public final class SoporteDtos {
 public record Archivo(String nombre,String mime,String base64){}
 public record NuevoTicket(String asunto,Categoria categoria,String contenido,Long operacionId,Long armaId,List<Archivo> adjuntos){}
 public record Mensaje(String contenido,List<Archivo> adjuntos){}
 public record Cambio(Long version,Estado estado,Prioridad prioridad,String motivo){}
 public record Asignacion(Long version,String adminId,String motivo){}
 public record AdjuntoVista(Long id,String nombre,String mime,Long mensajeId){}
 public record MensajeVista(Long id,String autor,String rol,String contenido,Instant fecha){}
 public record EventoVista(Long id,String autor,Instant fecha,String accion,String anterior,String nuevo,String motivo){}
 public record TicketVista(Long id,long version,String asunto,Categoria categoria,String estado,Prioridad prioridad,String asignado,Long operacionId,Long armaId,Instant creada,Instant actualizada,List<MensajeVista> mensajes,List<AdjuntoVista> adjuntos,List<EventoVista> eventos){}
 public record Aviso(Long id,String texto,String enlace,Instant fecha){}
 public record Fila(Long id,String clase,String titulo,String estado,String prioridad,String tipoObjetivo,Instant fecha,String asignado){}
 public record Pagina(List<Fila> filas,long total,int pagina,int tamano){}
}
