package cl.skinspad.reporte.soporte;
import jakarta.persistence.*;import lombok.*;import java.time.*;import static cl.skinspad.reporte.soporte.EstadosSoporte.*;
@Entity @Table(name="soporte_alerta_precio") @Getter @Setter @NoArgsConstructor
public class AlertaPrecio {
 @Id private Long productoId;
 @Version private long version;
 @Column(nullable=false,length=64) private String huella;
 @Column(length=64) private String huellaValidada;
 @Column(nullable=false,length=30) private String estado;
 @Column(length=200) private String nombre;
 private java.math.BigDecimal precio;private java.math.BigDecimal mediana;private int comparables;
 @Column(length=250) private String explicacion;
 @Column(length=80) private String adminId;
 @Column(length=1000) private String motivoRevision;
 private Instant fechaRevision;
 @Column(nullable=false) private Instant actualizada=Instant.now();
}
