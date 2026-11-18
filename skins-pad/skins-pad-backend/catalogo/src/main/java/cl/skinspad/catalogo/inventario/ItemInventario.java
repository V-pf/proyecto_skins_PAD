package cl.skinspad.catalogo.inventario;

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

  @Column(length = 1000)
  private String imagen;

  private boolean intercambiable;
  private boolean presente;
  private boolean demo;

  @Column(unique = true)
  private Long productoId;
}
