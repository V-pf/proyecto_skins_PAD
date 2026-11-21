package cl.skinspad.reporte.soporte;
import java.util.*;import jakarta.persistence.EntityManager;import lombok.RequiredArgsConstructor;import org.springframework.stereotype.Service;
@Service @RequiredArgsConstructor public class SoporteStats {
 private final EntityManager em;
 public record Grupo(String etiqueta,long cantidad){}
 public record Resumen(long pendientes,List<Grupo> tickets,List<Grupo> reportes,Double horasTicket,Double horasReporte,List<Grupo> motivos,long preciosPendientes,double porcentajeRechazados,List<Grupo> denunciados){}
 private long numero(String sql){return ((Number)em.createNativeQuery(sql).getSingleResult()).longValue();}
 private Double promedio(String tabla,String fin){var n=(Number)em.createNativeQuery("select avg(timestampdiff(SECOND,fecha_creacion,"+fin+"))/3600.0 from "+tabla+" where "+fin+" is not null").getSingleResult();return n==null?null:n.doubleValue();}
 private List<Grupo> grupos(String tabla,String columna){List<Object[]> rows=em.createNativeQuery("select "+columna+",count(*) from "+tabla+" group by "+columna+" order by count(*) desc, "+columna).setMaxResults(20).getResultList();return rows.stream().map(r->new Grupo((String)r[0],((Number)r[1]).longValue())).toList();}
 public Resumen obtener(){long pendientes=numero("select count(*) from soporte_reporte where estado in ('ABIERTO','EN_REVISION')");long rechazados=numero("select count(*) from soporte_reporte where estado='RECHAZADO'"),finalizados=numero("select count(*) from soporte_reporte where estado in ('RESUELTO','RECHAZADO')");return new Resumen(pendientes,List.of(),grupos("soporte_reporte","estado"),null,promedio("soporte_reporte","fecha_resolucion"),grupos("soporte_reporte","motivo"),numero("select count(*) from soporte_alerta_precio where estado='PENDIENTE'"),finalizados==0?0:100.0*rechazados/finalizados,grupos("soporte_reporte","denunciado_id"));}
}
