package cl.skinspad.pago.repository;

import cl.skinspad.pago.domain.ComprobanteTransferencia;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface ComprobanteRepository extends JpaRepository<ComprobanteTransferencia, Long> {
    Optional<ComprobanteTransferencia> findByPagoId(Long pagoId);
}
