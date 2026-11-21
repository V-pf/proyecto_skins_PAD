package cl.skinspad.pago.escrow;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
@Entity @Table(name="operaciones",uniqueConstraints={
 @UniqueConstraint(columnNames={"pagoId","productoId"}),
 @UniqueConstraint(name="uk_operacion_producto_ocupado",columnNames="producto_ocupado")
})
@Getter @Setter @NoArgsConstructor
public class Operacion {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Version private long version;
 @Column(nullable=false) private Long pagoId;
 @Column(nullable=false) private Long productoId;
 // Se adquiere al crear el pago y se libera al cancelar o reembolsar la línea.
 @com.fasterxml.jackson.annotation.JsonIgnore
 @Column(name="producto_ocupado") private Long productoOcupado;
 @Column(nullable=false,length=80) private String compradorId;
 @Column(nullable=false,length=80) private String vendedorId;
 @Column(nullable=false) private String nombre;
 private String urlIntercambio;
 @Column(nullable=false) private long precio;
 @Column(nullable=false) private long comision;
 @Column(nullable=false) private int comisionBps=500;
 @Column(nullable=false) private String pagaComision="COMPRADOR";
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=40) private EstadoOperacion estado=EstadoOperacion.PAGO_PENDIENTE;
 private String causa;
 private boolean retenido;
 private boolean historica;
 private boolean sincronizado=true;
 @Column(length=500) private String errorSincronizacion;
 @Column(nullable=false) private Instant creada=Instant.now();
 private Instant actualizada=Instant.now();
 private Instant vence;
 private Instant completada;
 // Persistido antes de cualquier efecto remoto. Impide acciones incompatibles durante recuperación.
 @Column(length=40) private String accionPendiente;
 @Column(length=80) private String actorPendiente;
 @Column(length=1000) private String motivoPendiente;
}
