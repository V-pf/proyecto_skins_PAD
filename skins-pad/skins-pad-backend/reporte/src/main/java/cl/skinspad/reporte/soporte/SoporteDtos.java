package cl.skinspad.reporte.soporte;
import java.util.*;import java.time.*;import static cl.skinspad.reporte.soporte.EstadosSoporte.*;
public final class SoporteDtos {
 public record Archivo(String nombre,String mime,String base64){}
 public record NuevoReporte(Objetivo tipoObjetivo,String objetivoId,Motivo motivo,String descripcion,List<String> evidenciaUrls,List<Archivo> adjuntos){}
 public record Asignacion(Long version,String adminId,String motivo){}
 public record Resolver(Long version,Estado estado,Accion accion,String motivo,String resumenPublico,Boolean confirmado){}
 public record ValidarPrecio(Long version,String motivo,Boolean confirmado){}
 public record AdjuntoVista(Long id,String nombre,String mime,Long mensajeId){}
 public record EventoVista(Long id,String autor,Instant fecha,String accion,String anterior,String nuevo,String motivo){}
 public record ReporteVista(Long id,long version,Objetivo tipoObjetivo,String objetivoId,Motivo motivo,String descripcion,String estado,Prioridad prioridad,String asignado,Instant creada,String resolucion,Accion accionTomada,boolean accionPendiente,String errorAccion,List<String> evidencia,List<AdjuntoVista> adjuntos){}
 public record ContextoReporte(ReporteVista reporte,Object objetivo,AlertaVista precio,long rechazosReportante,List<ReporteVista> otrosObjetivo,List<ReporteVista> historialDenunciado,List<EventoVista> eventos,List<Accion> acciones){}
 public record AlertaVista(Long productoId,long version,String nombre,String estado,java.math.BigDecimal precio,java.math.BigDecimal mediana,int comparables,String explicacion){}
 public record Aviso(Long id,String texto,String enlace,Instant fecha){}
 public record Fila(Long id,String clase,String titulo,String estado,String prioridad,String tipoObjetivo,Instant fecha,String asignado){}
 public record Pagina(List<Fila> filas,long total,int pagina,int tamano){}
}
