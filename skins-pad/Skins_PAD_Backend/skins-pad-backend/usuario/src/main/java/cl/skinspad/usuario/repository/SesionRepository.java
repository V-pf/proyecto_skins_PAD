package cl.skinspad.usuario.repository;

import cl.skinspad.usuario.domain.Sesion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface SesionRepository extends JpaRepository<Sesion, String> {
    Optional<Sesion> findByTokenAndCierreIsNull(String token);
}
