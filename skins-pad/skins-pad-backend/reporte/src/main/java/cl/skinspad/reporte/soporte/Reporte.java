package cl.skinspad.reporte.soporte;
import jakarta.persistence.*;import lombok.*;import java.time.*;import static cl.skinspad.reporte.soporte.EstadosSoporte.*;
@Entity @Table(name="soporte_reporte",indexes={@Index(name="ix_reporte_usuario_fecha",columnList="reportanteId,fechaCreacion"),@Index(name="ix_reporte_objetivo",columnList="tipoObjetivo,objetivoId"),@Index(name="ix_reporte_estado_prioridad",columnList="estado,prioridad,fechaCreacion")}) @Getter @Setter @NoArgsConstructor
public class Reporte {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Version private long version;
 @Column(nullable=false,length=80) private String reportanteId;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private Objetivo tipoObjetivo;
 @Column(nullable=false,length=80) private String objetivoId;
 @Column(nullable=false,length=80) private String denunciadoId;
 @Column(unique=true,length=255) private String claveAbierta;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=60) private Motivo motivo;
 @Column(nullable=false,length=4000) private String descripcion;
 @Column(length=1600) private String evidenciaUrls;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private Estado estado=Estado.ABIERTO;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private Prioridad prioridad=Prioridad.NORMAL;
 @Column(nullable=false) private Instant fechaCreacion=Instant.now();
 private Instant fechaResolucion;
 @Column(length=80) private String adminResolutorId;
 @Column(length=80) private String adminAsignadoId;
 @Column(length=1000) private String resolucion;
 @Enumerated(EnumType.STRING) @Column(length=40) private Accion accionTomada;
 @Column(length=1000) private String motivoInterno;
 @Column(length=80) private String referenciaAccion;
 @Enumerated(EnumType.STRING) @Column(length=20) private Estado estadoSolicitado;
 @Column(nullable=false) private boolean accionPendiente;
 @Column(length=300) private String errorAccion;
}
