package cl.skinspad.reporte.soporte;
import java.util.*;import org.springframework.data.jpa.repository.*;
public interface ReporteRepository extends JpaRepository<Reporte,Long>{
List<Reporte> findByReportanteIdOrderByFechaCreacionDesc(String id); List<Reporte> findByTipoObjetivoAndObjetivoId(EstadosSoporte.Objetivo tipo,String id); List<Reporte> findByAccionPendienteTrue(); boolean existsByClaveAbierta(String clave);
@Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE) @Query("select x from Reporte x where x.id=:id") Optional<Reporte> bloquear(@org.springframework.data.repository.query.Param("id") Long id);
}