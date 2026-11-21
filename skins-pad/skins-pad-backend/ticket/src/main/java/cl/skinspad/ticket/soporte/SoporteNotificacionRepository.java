package cl.skinspad.ticket.soporte;
import java.util.*;import org.springframework.data.jpa.repository.*;
public interface SoporteNotificacionRepository extends JpaRepository<SoporteNotificacion,Long>{
List<SoporteNotificacion> findTop50ByDestinatarioOrderByIdDesc(String id); boolean existsByClave(String clave);
@Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE) @Query("select x from SoporteNotificacion x where x.id=:id") Optional<SoporteNotificacion> bloquear(@org.springframework.data.repository.query.Param("id") Long id);
}