package cl.skinspad.pago.repository;

import cl.skinspad.pago.domain.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface PagoRepository extends JpaRepository<Pago, Long> {
    Optional<Pago> findByPedidoId(String pedidoId);
    List<Pago> findAllByUsuarioIdOrderByFechaDesc(String usuarioId);
}
