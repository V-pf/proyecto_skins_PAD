package cl.skinspad.ticket.soporte;
import java.util.*;import org.springframework.data.jpa.repository.*;
public interface TicketRepository extends JpaRepository<Ticket,Long>{
List<Ticket> findByUsuarioIdOrderByFechaCreacionDesc(String id);
@Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE) @Query("select x from Ticket x where x.id=:id") Optional<Ticket> bloquear(@org.springframework.data.repository.query.Param("id") Long id);
}