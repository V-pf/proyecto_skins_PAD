package cl.skinspad.usuario.moderacion;
import jakarta.persistence.*;import lombok.*;import java.time.Instant;
@Entity @Table(name="moderacion_reporte") @Getter @NoArgsConstructor @org.hibernate.annotations.Immutable
public class AccionReporte {
 @Id @Column(length=80,updatable=false) private String referencia;
 @Column(nullable=false,length=80,updatable=false) private String objetivo;
 @Column(nullable=false,length=80,updatable=false) private String adminId;
 @Column(nullable=false,length=1000,updatable=false) private String motivo;
 @Column(nullable=false,length=40,updatable=false) private String accion;
 @Column(nullable=false,updatable=false) private Instant fecha=Instant.now();
 public AccionReporte(String r,String o,String a,String m,String ac){referencia=r;objetivo=o;adminId=a;motivo=m;accion=ac;}
}
