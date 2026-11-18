package cl.skinspad.catalogo.inventario;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface VinculoRepository extends JpaRepository<VinculoSteam, String> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select v from VinculoSteam v where v.usuarioId=:id")
  Optional<VinculoSteam> bloquear(@Param("id") String id);

  boolean existsBySteamId(String steamId);
}
