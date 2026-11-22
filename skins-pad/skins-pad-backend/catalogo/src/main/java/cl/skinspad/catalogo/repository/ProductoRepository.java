package cl.skinspad.catalogo.repository;

import cl.skinspad.catalogo.domain.EstadoProducto;
import cl.skinspad.catalogo.domain.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Set;
public interface ProductoRepository extends JpaRepository<Producto, Long> {
    @Modifying
    @Query("""
        update Producto p set p.inventarioId = p.inventarioOcupadoId
        where p.inventarioId is null and p.inventarioOcupadoId is not null
        """)
    void migrarReferenciasDeInventario();

    @Modifying
    @Query("""
        update Producto p set p.inventarioOcupadoId = null
        where p.inventarioId is not null and p.inventarioOcupadoId is not null
          and p.reservaPago is null and (p.revisionApta is null or p.revisionApta = true)
          and p.estado in :retiradas
        """)
    void liberarInventarioDePublicacionesRetiradas(@Param("retiradas") Set<EstadoProducto> retiradas);

    @Modifying
    @Query("""
        update Producto p set p.estado = :activo
        where p.estado = :pendiente and p.inventarioId is not null
          and p.reservaPago is null and p.trasladoPendiente = false
          and (p.revisionApta is null or p.revisionApta = true)
          and p.categoria.id in (select c.id from Categoria c where c.estadoHabilitada = true)
        """)
    void activarPendientesDeInventario(@Param("pendiente") EstadoProducto pendiente,
            @Param("activo") EstadoProducto activo);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select p from Producto p where p.id = :id")
    java.util.Optional<Producto> bloquear(@org.springframework.data.repository.query.Param("id") Long id);
    boolean existsByInventarioOcupadoId(Long inventarioId);
    @org.springframework.data.jpa.repository.Query("""
        select p.id from Producto p
        where (p.trasladoPendiente is null or p.trasladoPendiente = true)
          and p.vendedorId is not null and trim(p.vendedorId) <> ''
        order by p.id
        """)
    List<Long> buscarPendientesDeTraslado();
    List<Producto> findByVendedorIdAndInventarioIdIsNotNullOrderById(String vendedorId);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select p from Producto p where p.vendedorId = :id and p.inventarioOcupadoId is not null order by p.id")
    List<Producto> bloquearPublicaciones(@org.springframework.data.repository.query.Param("id") String id);
    boolean existsByCategoria_Id(Long categoriaId);
    List<Producto> findByVendedorId(String vendedorId);
}
