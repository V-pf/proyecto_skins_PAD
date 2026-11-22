package cl.skinspad.catalogo.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

// Publicación de una skin con su vendedor, categoría, precio y datos de desgaste.
@Entity
@Table(name = "productos", indexes = @Index(name = "idx_productos_item_inventario", columnList = "item_inventario_id"))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String vendedorId;
    @Column(name = "item_inventario_id")
    private Long inventarioId;
    // La columna original conserva la restricción única para las publicaciones que ocupan el ítem.
    @com.fasterxml.jackson.annotation.JsonIgnore
    @Column(name = "inventario_id", unique = true)
    private Long inventarioOcupadoId;
    private Boolean demo = false;
    // NULL identifica las publicaciones que existían antes del traslado inicial.
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Boolean trasladoPendiente = false;
    // Al liberar una compra anterior al traslado, la skin queda disponible en inventario.
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Boolean retiradaParaInventario = false;
    // Guarda la referencia del pago ligado a la reserva o la venta.
    private String reservaPago;
    private Boolean revisionApta;
    @Column(length=500) private String motivoRechazo;
    @Column(length=500) private String urlIntercambio;
    private String vendedor;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @NotBlank
    @Column(nullable = false, length = 160)
    private String nombre;

    @Column(length = 1500)
    private String descripcion;

    @NotNull
    @DecimalMin("1.00")
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal precio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoProducto estado = EstadoProducto.PENDIENTE_APROBACION;

    @Column(nullable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    private String tipo;
    private String grado;
    private String desgaste;

    @Column(nullable = false)
    private Boolean stattrak = false;

    @Column(name = "float_valor", precision = 8, scale = 5)
    private BigDecimal floatValor;

    // Las imágenes pertenecen a la publicación y se guardan junto con ella.
    @OneToMany(
        mappedBy = "producto",
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.EAGER
    )
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<ImagenProducto> imagenes = new ArrayList<>();

    public boolean permiteRepublicar() {
        return reservaPago == null && !Boolean.FALSE.equals(revisionApta)
                && EstadoProducto.RETIRADAS.contains(estado);
    }

    @PrePersist
    @PreUpdate
    private void actualizarOcupacionInventario() {
        inventarioOcupadoId = permiteRepublicar() ? null : inventarioId;
    }
}
