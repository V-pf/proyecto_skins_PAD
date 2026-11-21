package cl.skinspad.reporte.soporte;
import java.util.*;import org.springframework.data.jpa.repository.*;
public interface AlertaPrecioRepository extends JpaRepository<AlertaPrecio,Long>{

@Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE) @Query("select x from AlertaPrecio x where x.productoId=:id") Optional<AlertaPrecio> bloquear(@org.springframework.data.repository.query.Param("id") Long id);
}