package cl.skinspad.reporte.soporte;
import jakarta.persistence.*;import lombok.*;import java.time.*;import static cl.skinspad.reporte.soporte.EstadosSoporte.*;
@Entity @Table(name="soporte_cuota") @Getter @Setter @NoArgsConstructor
public class SoporteCuota {
 @Id @Column(length=80) private String usuarioId;
 @Column(nullable=false) private LocalDate dia;
 private int reportes;
 public SoporteCuota(String id){usuarioId=id;dia=LocalDate.now(ZoneId.of("America/Santiago"));}
}
