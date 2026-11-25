package cl.skinspad.usuario.repository;

import cl.skinspad.usuario.domain.Sesion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface SesionRepository extends JpaRepository<Sesion, String> {
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("""
        update Sesion s set s.ultimaRenovacion = :ahora
        where s.token = :sid and s.cierre is null and s.ultimaRenovacion >= :limite
        """)
    int renovar(String sid, java.time.LocalDateTime ahora, java.time.LocalDateTime limite);

    Optional<Sesion> findByTokenAndCierreIsNull(String token);
}
