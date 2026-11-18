package cl.skinspad.pago.escrow;
public interface EventoRepository extends org.springframework.data.jpa.repository.JpaRepository<EventoOperacion,Long> {
 java.util.List<EventoOperacion> findByOperacionIdOrderById(Long id);
}
