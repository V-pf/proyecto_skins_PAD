package cl.skinspad.ticket.soporte;
import java.util.*;import org.springframework.data.jpa.repository.*;
public interface TicketMensajeRepository extends JpaRepository<TicketMensaje,Long>{
List<TicketMensaje> findByTicket_IdOrderById(Long id);
@Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE) @Query("select x from TicketMensaje x where x.id=:id") Optional<TicketMensaje> bloquear(@org.springframework.data.repository.query.Param("id") Long id);
}