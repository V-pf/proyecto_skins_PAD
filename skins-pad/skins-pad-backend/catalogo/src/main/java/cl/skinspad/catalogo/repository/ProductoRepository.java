package cl.skinspad.catalogo.repository;

import cl.skinspad.catalogo.domain.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ProductoRepository extends JpaRepository<Producto, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select p from Producto p where p.id = :id")
    java.util.Optional<Producto> bloquear(@org.springframework.data.repository.query.Param("id") Long id);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select p from Producto p where p.inventarioId = :id")
    java.util.Optional<Producto> bloquearPorInventario(@org.springframework.data.repository.query.Param("id") Long id);
    @org.springframework.data.jpa.repository.Query("""
        select p.id from Producto p
        where (p.trasladoPendiente is null or p.trasladoPendiente = true)
          and p.vendedorId is not null and trim(p.vendedorId) <> ''
        order by p.id
        """)
    List<Long> buscarPendientesDeTraslado();
    List<Producto> findByVendedorIdAndInventarioIdIsNotNullOrderById(String vendedorId);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select p from Producto p where p.vendedorId = :id and p.inventarioId is not null order by p.id")
    List<Producto> bloquearPublicaciones(@org.springframework.data.repository.query.Param("id") String id);
    boolean existsByCategoria_Id(Long categoriaId);
    List<Producto> findByVendedorId(String vendedorId);
}
