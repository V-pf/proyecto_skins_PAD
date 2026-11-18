package cl.skinspad.catalogo.inventario;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class VinculoSteam {
  @Id private String usuarioId;

  @Column(unique = true)
  private String steamId;

  private boolean demo;
  private Instant sincronizado;
}
