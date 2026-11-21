package cl.skinspad.ticket.soporte;
import java.util.*;import org.springframework.data.jpa.repository.*;
public interface SoporteCuotaRepository extends JpaRepository<SoporteCuota,String>{

@Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE) @Query("select x from SoporteCuota x where x.usuarioId=:id") Optional<SoporteCuota> bloquear(@org.springframework.data.repository.query.Param("id") String id);
}