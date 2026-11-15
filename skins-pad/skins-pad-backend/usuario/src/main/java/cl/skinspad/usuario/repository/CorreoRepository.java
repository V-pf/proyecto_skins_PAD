package cl.skinspad.usuario.repository;

import cl.skinspad.usuario.domain.CorreoPendiente;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CorreoRepository extends JpaRepository<CorreoPendiente, String> {
  List<CorreoPendiente> findTop50ByEnviadoFalseOrderByReferencia();
}
