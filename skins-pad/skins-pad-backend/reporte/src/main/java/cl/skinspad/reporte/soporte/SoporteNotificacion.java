package cl.skinspad.reporte.soporte;
import jakarta.persistence.*;import lombok.*;import java.time.*;import static cl.skinspad.reporte.soporte.EstadosSoporte.*;
@Entity @Table(name="soporte_notificacion",indexes=@Index(name="ix_soporte_notificacion",columnList="destinatario,fecha")) @Getter @NoArgsConstructor
public class SoporteNotificacion {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,unique=true,length=160) private String clave;
 @Column(nullable=false,length=80) private String destinatario;
 @Column(nullable=false,length=200) private String texto;
 @Column(nullable=false,length=120) private String enlace;
 @Column(nullable=false) private Instant fecha=Instant.now();
 public SoporteNotificacion(String clave,String para,String texto,String enlace){this.clave=clave;destinatario=para;this.texto=texto;this.enlace=enlace;}
}
