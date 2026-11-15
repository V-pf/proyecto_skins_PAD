package cl.skinspad.carrito.repository;

import cl.skinspad.carrito.domain.Carrito;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface CarritoRepository extends JpaRepository<Carrito, Long> {
    List<Carrito> findByUsuarioId(String usuarioId);
    Optional<Carrito> findTopByUsuarioIdOrderByFechaCreacionDesc(String usuarioId);
}
