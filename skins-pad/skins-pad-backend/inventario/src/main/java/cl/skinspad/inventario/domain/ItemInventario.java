package cl.skinspad.inventario.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"usuarioId", "assetId"}))
public class ItemInventario {
  private static final String PREFIJO_CATALOGO = "catalogo-";
  private static final String PREFIJO_PRECARGA = "precarga-";

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

  @Column(precision = 8, scale = 5)
  private java.math.BigDecimal floatValor;

  private boolean intercambiable;
  private boolean presente;
  @Transient private Long productoId;

  public static String assetDeCatalogo(Long productoId) {
    return PREFIJO_CATALOGO + productoId;
  }

  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  public Long getProductoOrigenId() {
    if (assetId == null || !assetId.startsWith(PREFIJO_CATALOGO)) return null;
    int inicio = PREFIJO_CATALOGO.length();
    int fin = assetId.indexOf('-', inicio);
    try {
      return Long.valueOf(assetId.substring(inicio, fin == -1 ? assetId.length() : fin));
    } catch (NumberFormatException e) {
      return null;
    }
  }

  public boolean esPrecarga() {
    return assetId != null && assetId.startsWith(PREFIJO_PRECARGA);
  }
}
