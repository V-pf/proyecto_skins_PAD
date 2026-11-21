package cl.skinspad.reporte.soporte;
import java.util.*;import org.springframework.data.jpa.repository.*;
public interface SoporteAdjuntoRepository extends JpaRepository<SoporteAdjunto,Long>{
List<SoporteAdjunto> findByEntidadAndEntidadId(String entidad,Long id);
@Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE) @Query("select x from SoporteAdjunto x where x.id=:id") Optional<SoporteAdjunto> bloquear(@org.springframework.data.repository.query.Param("id") Long id);
}