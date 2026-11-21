package cl.skinspad.reporte.soporte;
import jakarta.persistence.*;import lombok.*;import java.time.*;import static cl.skinspad.reporte.soporte.EstadosSoporte.*;
@Entity @Table(name="soporte_adjunto",indexes=@Index(name="ix_soporte_adjunto",columnList="entidad,entidadId")) @Getter @Setter @NoArgsConstructor
public class SoporteAdjunto {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,length=15) private String entidad;
 @Column(nullable=false) private Long entidadId;
 private Long mensajeId;
 @Column(nullable=false,length=100) private String nombre;
 @Column(nullable=false,length=30) private String mime;
 @Lob @Column(nullable=false,columnDefinition="LONGBLOB") private byte[] datos;
}
