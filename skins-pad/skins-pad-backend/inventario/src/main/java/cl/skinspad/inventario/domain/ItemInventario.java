package cl.skinspad.inventario.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"usuarioId", "assetId"}))
public class ItemInventario {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String usuarioId;

  @Column(nullable = false)
  private String assetId;

  @Column(nullable = false)
  private String nombre;

  private String tipo;
  private String grado;
  private String desgaste;
  private boolean stattrak;

  @Column(columnDefinition = "TEXT")
  private String imagen;

  private Long productoOrigenId;

  @Column(precision = 8, scale = 5)
  private java.math.BigDecimal floatValor;

  private boolean intercambiable;
  private boolean presente;
  private boolean demo;
  @Transient private Long productoId;

}
