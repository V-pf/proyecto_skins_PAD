package cl.skinspad.steam.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class VinculoSteam {
  @Id private String usuarioId;

  @Column(unique = true, nullable = false)
  private String steamId;

}
