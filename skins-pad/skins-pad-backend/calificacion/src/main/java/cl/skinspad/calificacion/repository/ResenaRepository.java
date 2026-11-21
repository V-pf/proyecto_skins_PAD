package cl.skinspad.calificacion.repository;
import cl.skinspad.calificacion.domain.Resena;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ResenaRepository extends JpaRepository<Resena,Long> {
    List<Resena> findByVendedorIdOrderByIdAsc(String vendedorId);
    boolean existsByOperacionId(Long operacionId);
}
