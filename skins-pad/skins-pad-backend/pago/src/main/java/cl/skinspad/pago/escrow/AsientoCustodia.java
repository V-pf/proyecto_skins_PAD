package cl.skinspad.pago.escrow;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
@Entity @Table(name="custodia_asientos",uniqueConstraints=@UniqueConstraint(columnNames={"operacionId","tipo"})) @Getter @NoArgsConstructor
public class AsientoCustodia {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,updatable=false) private Long operacionId;
 @Column(nullable=false,updatable=false,length=30) private String tipo;
 @Column(nullable=false,updatable=false) private long monto;
 @Column(nullable=false,updatable=false,length=80) private String actor;
 @Column(nullable=false,updatable=false) private Instant fecha=Instant.now();
 public AsientoCustodia(Operacion o,String tipo,long monto,String actor){this.operacionId=o.getId();this.tipo=tipo;this.monto=monto;this.actor=actor;}
}
