package cl.skinspad.pago.escrow;
import java.util.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
public interface OperacionRepository extends JpaRepository<Operacion,Long> {
 @Query("select o.pagoId from Operacion o where o.id=:id") Long pagoId(@Param("id") Long id);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select o from Operacion o where o.id=:id") Optional<Operacion> bloquear(@Param("id") Long id);
 List<Operacion> findByPagoIdOrderById(Long pagoId);
 List<Operacion> findByCompradorIdOrderByIdDesc(String id);
 List<Operacion> findByVendedorIdOrderByIdDesc(String id);
 @Query("select o.id from Operacion o where o.accionPendiente is not null or (o.vence<=:now and o.estado in (cl.skinspad.pago.escrow.EstadoOperacion.PAGO_PENDIENTE,cl.skinspad.pago.escrow.EstadoOperacion.PAGO_RETENIDO,cl.skinspad.pago.escrow.EstadoOperacion.ACEPTADA_POR_VENDEDOR,cl.skinspad.pago.escrow.EstadoOperacion.ENTREGADA)) order by o.id")
 List<Long> pendientes(@Param("now") java.time.Instant now,org.springframework.data.domain.Pageable page);
}
