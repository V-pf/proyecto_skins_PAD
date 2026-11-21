package cl.skinspad.ticket.soporte;
import jakarta.persistence.*;import lombok.*;import java.time.*;import static cl.skinspad.ticket.soporte.EstadosSoporte.*;
@Entity @Table(name="soporte_evento",indexes=@Index(name="ix_soporte_evento",columnList="entidad,entidadId,fecha")) @org.hibernate.annotations.Immutable @Getter @NoArgsConstructor
public class SoporteEvento {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,updatable=false,length=15) private String entidad;
 @Column(nullable=false,updatable=false) private Long entidadId;
 @Column(nullable=false,updatable=false,length=80) private String autor;
 @Column(nullable=false,updatable=false) private Instant fecha=Instant.now();
 @Column(nullable=false,updatable=false,length=40) private String accion;
 @Column(updatable=false,length=200) private String anterior;
 @Column(updatable=false,length=200) private String nuevo;
 @Column(updatable=false,length=1200) private String motivo;
 public SoporteEvento(String e,Long id,String actor,String accion,Object a,Object b,String m){entidad=e;entidadId=id;autor=actor;this.accion=accion;anterior=a==null?null:a.toString();nuevo=b==null?null:b.toString();motivo=m;}
}
