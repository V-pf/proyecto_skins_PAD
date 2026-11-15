package cl.skinspad.usuario.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Vincula un token con una cuenta y guarda las fechas de inicio y cierre.
@Entity
@Table(name = "sesiones")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Sesion {

    @Id
    @Column(length = 64)
    private String token;

    @Column(nullable = false, length = 40)
    private String usuarioId;

    @Column(nullable = false)
    private LocalDateTime inicio;

    private LocalDateTime cierre;
}
