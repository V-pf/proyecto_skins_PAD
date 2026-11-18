package cl.skinspad.carrito.repository;

import cl.skinspad.carrito.domain.ItemCarrito;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ItemCarritoRepository extends JpaRepository<ItemCarrito, Long> {
}
