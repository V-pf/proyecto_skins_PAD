package cl.skinspad.usuario.repository;
import cl.skinspad.usuario.domain.Movimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface MovimientoRepository extends JpaRepository<Movimiento,Long>{
    Optional<Movimiento> findByCuentaAndReferencia(String cuenta,String referencia);
    List<Movimiento> findByCuentaAndTipoOrderByFechaDesc(String cuenta,String tipo);
}
