package cl.skinspad.ticket.soporte;
import jakarta.persistence.*;import lombok.*;import java.time.*;import static cl.skinspad.ticket.soporte.EstadosSoporte.*;
@Entity @Table(name="soporte_ticket_mensaje",indexes=@Index(name="ix_mensaje_ticket",columnList="ticket_id,fecha")) @Getter @Setter @NoArgsConstructor
public class TicketMensaje {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="ticket_id") private Ticket ticket;
 @Column(nullable=false,length=80) private String autorId;
 @Column(nullable=false,length=20) private String rolAutor;
 @Column(nullable=false,length=4000) private String contenido;
 @Column(nullable=false) private Instant fecha=Instant.now();
}
