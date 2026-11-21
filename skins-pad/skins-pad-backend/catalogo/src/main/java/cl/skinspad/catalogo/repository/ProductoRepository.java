package cl.skinspad.catalogo.repository;

import cl.skinspad.catalogo.domain.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ProductoRepository extends JpaRepository<Producto, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select p from Producto p where p.id = :id")
    java.util.Optional<Producto> bloquear(@org.springframework.data.repository.query.Param("id") Long id);
    boolean existsByCategoria_Id(Long categoriaId);
    List<Producto> findByVendedorId(String vendedorId);
}
