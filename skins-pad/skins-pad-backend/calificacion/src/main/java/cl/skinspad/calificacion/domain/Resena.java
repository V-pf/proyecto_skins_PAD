package cl.skinspad.calificacion.domain;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
@Entity
@Getter @Setter @NoArgsConstructor
@Table(name="resenas", uniqueConstraints=@UniqueConstraint(columnNames="operacionId"))
public class Resena {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false) private Long operacionId;
    @Column(nullable=false) private String vendedorId;
    @Column(nullable=false) private String compradorId;
    @Column(nullable=false, precision=3, scale=2) private BigDecimal puntaje;
    @Column(nullable=false, length=1000) private String comentario;
    @Column(nullable=false) private Instant fecha;
}
