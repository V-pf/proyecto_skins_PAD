package cl.skinspad.ticket.soporte;
import jakarta.persistence.*;import lombok.*;import java.time.*;import static cl.skinspad.ticket.soporte.EstadosSoporte.*;
@Entity @Table(name="soporte_ticket",indexes={@Index(name="ix_ticket_usuario_fecha",columnList="usuarioId,fechaCreacion"),@Index(name="ix_ticket_estado_prioridad",columnList="estado,prioridad,fechaCreacion")}) @Getter @Setter @NoArgsConstructor
public class Ticket {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Version private long version;
 @Column(nullable=false,length=80) private String usuarioId;
 @Column(nullable=false,length=160) private String asunto;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private Categoria categoria;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private Estado estado=Estado.ABIERTO;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private Prioridad prioridad=Prioridad.NORMAL;
 @Column(nullable=false) private Instant fechaCreacion=Instant.now();
 @Column(nullable=false) private Instant fechaActualizacion=Instant.now();
 private Instant fechaCierre;
 @Column(length=80) private String adminAsignadoId;
 private Long operacionId;private Long armaId;
}
