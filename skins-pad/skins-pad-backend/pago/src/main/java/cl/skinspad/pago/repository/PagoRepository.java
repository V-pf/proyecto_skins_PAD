package cl.skinspad.pago.repository;

import cl.skinspad.pago.domain.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface PagoRepository extends JpaRepository<Pago, Long> {
    @org.springframework.data.jpa.repository.Query("select p from Pago p where (:desde is null or p.fecha >= :desde) and p.fecha < :hasta order by p.fecha")
    List<Pago> findForStats(@org.springframework.data.repository.query.Param("desde") java.time.LocalDateTime desde,
        @org.springframework.data.repository.query.Param("hasta") java.time.LocalDateTime hasta);
    Optional<Pago> findByPedidoId(String pedidoId);
    List<Pago> findAllByUsuarioIdOrderByFechaDesc(String usuarioId);
}
