package cl.skinspad.carrito.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "items_carrito", uniqueConstraints = @UniqueConstraint(columnNames = {
        "carrito_id", "producto_id"
}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemCarrito {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "producto_id", nullable = false)
    private Long productoId;
    @Column(nullable = false, length = 160)
    private String nombreItem;
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal precio;
    @Column(nullable = false)
    private int cantidad = 1;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "carrito_id", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Carrito carrito;
}
