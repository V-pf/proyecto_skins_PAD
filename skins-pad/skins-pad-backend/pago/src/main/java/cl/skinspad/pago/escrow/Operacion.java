package cl.skinspad.pago.escrow;
import cl.skinspad.pago.domain.Pago;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
@Entity @Table(name="operaciones",uniqueConstraints=@UniqueConstraint(columnNames={"pago_id","producto_id"}))
@Getter @Setter @NoArgsConstructor
public class Operacion {
 public static final String PENDIENTE_IMPORTACION = "LEGACY_PENDIENTE_IMPORTACION";

 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Version private long version;
 @JsonIgnore
 @ManyToOne(fetch=FetchType.LAZY, optional=false)
 @JoinColumn(name="pago_id", nullable=false, foreignKey=@ForeignKey(ConstraintMode.NO_CONSTRAINT))
 private Pago pago;
 @Setter(AccessLevel.NONE)
 @Column(name="pago_id", insertable=false, updatable=false) private Long pagoId;
 @Column(name="producto_id", nullable=false) private Long productoId;
 @Column(nullable=false,length=80) private String compradorId;
 @Column(nullable=false,length=80) private String vendedorId;
 @Column(nullable=false) private String nombre;
 @JsonIgnore
 @Column(precision=21, scale=2) private BigDecimal precio;
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

 public Long getPagoId() {
   return pagoId != null || pago == null ? pagoId : pago.getId();
 }

 public long precioEnPesos() {
   return precio.longValueExact();
 }

 @JsonGetter("precio")
 public BigDecimal precioEnRespuesta() {
   return precio == null ? BigDecimal.ZERO : precio;
 }

 public static Operacion pendienteDeImportar(Long productoId, String compradorId, BigDecimal precio, Instant fecha) {
   var operacion = new Operacion();
   operacion.setProductoId(productoId);
   operacion.setCompradorId(compradorId);
   operacion.setVendedorId("LEGACY_DESCONOCIDO");
   operacion.setNombre("Skin #" + productoId);
   operacion.setPrecio(precio);
   operacion.setComisionBps(-1);
   operacion.setCreada(fecha);
   operacion.setActualizada(fecha);
   operacion.setHistorica(true);
   operacion.setEstado(EstadoOperacion.CONGELADA);
   operacion.setCausa(PENDIENTE_IMPORTACION);
   return operacion;
 }
}
