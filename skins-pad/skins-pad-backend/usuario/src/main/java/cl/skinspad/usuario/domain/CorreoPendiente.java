package cl.skinspad.usuario.domain;

import jakarta.persistence.*;
import lombok.*;

// Correo guardado para enviarlo después del registro, la verificación o una compra.
@Entity
@Getter
@Setter
@NoArgsConstructor
public class CorreoPendiente {
  @Id private String referencia;
  private String destinatario;
  private String asunto;

  @Column(columnDefinition = "TEXT")
  private String contenido;

  private boolean enviado;
}
