package cl.skinspad.steam.repository;

import cl.skinspad.steam.domain.SteamEstado;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface SteamEstadoRepository extends JpaRepository<SteamEstado, String> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from SteamEstado e where e.estado=:estado")
  Optional<SteamEstado> bloquear(@Param("estado") String estado);

  void deleteByVenceBefore(java.time.Instant fecha);
}
