package cl.skinspad.inventario.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class SincronizacionInventario {
  @Id private String usuarioId;

  private Instant sincronizado;
}
