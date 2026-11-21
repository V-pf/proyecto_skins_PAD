package cl.skinspad.steam.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class SteamEstado {
  @Id private String estado;

  @Column(nullable = false)
  private String usuarioId;

  @Column(nullable = false)
  private Instant vence;
}
