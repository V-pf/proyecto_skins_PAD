package cl.skinspad.inventario.repository;

import cl.skinspad.inventario.domain.SincronizacionInventario;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface SincronizacionRepository extends JpaRepository<SincronizacionInventario, String> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select s from SincronizacionInventario s where s.usuarioId = :id")
  Optional<SincronizacionInventario> bloquear(@Param("id") String id);
}
