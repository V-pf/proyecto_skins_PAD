package cl.skinspad.pago.escrow;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
@Entity @Table(name="operacion_eventos") @Getter @NoArgsConstructor
public class EventoOperacion {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,updatable=false) private Long operacionId;
 @Column(nullable=false,updatable=false,length=80) private String actor;
 @Column(nullable=false,updatable=false,length=40) private String accion;
 @Column(nullable=false,updatable=false,length=40) private String estado;
 @Column(updatable=false,length=1000) private String motivo;
 @Column(nullable=false,updatable=false) private Instant fecha=Instant.now();
 public EventoOperacion(Operacion o,String actor,String accion,String motivo){this.operacionId=o.getId();this.actor=actor;this.accion=accion;this.estado=o.getEstado().name();this.motivo=motivo;}
}
