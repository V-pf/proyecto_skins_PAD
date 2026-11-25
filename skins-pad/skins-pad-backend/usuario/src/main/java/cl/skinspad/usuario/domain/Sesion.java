package cl.skinspad.usuario.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

import lombok.Data;
import lombok.NoArgsConstructor;

// Vincula un token con una cuenta y guarda las fechas de inicio y cierre.
@Entity
@Table(name = "sesiones")
@Data
@NoArgsConstructor

public class Sesion {

    @Id
    @Column(length = 64)
    private String token;

    @Column(nullable = false, length = 40)
    private String usuarioId;

    @Column(nullable = false)
    private LocalDateTime inicio;

    private LocalDateTime cierre;
    private LocalDateTime ultimaRenovacion;

    public Sesion(String token, String usuarioId, LocalDateTime inicio, LocalDateTime cierre) {
        this.token = token;
        this.usuarioId = usuarioId;
        this.inicio = inicio;
        this.cierre = cierre;
        this.ultimaRenovacion = inicio;
    }
}
