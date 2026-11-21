package cl.skinspad.reporte.soporte;
import java.util.*;import org.springframework.data.jpa.repository.*;
public interface SoporteEventoRepository extends JpaRepository<SoporteEvento,Long>{
List<SoporteEvento> findByEntidadAndEntidadIdOrderById(String entidad,Long id);
@Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE) @Query("select x from SoporteEvento x where x.id=:id") Optional<SoporteEvento> bloquear(@org.springframework.data.repository.query.Param("id") Long id);
}