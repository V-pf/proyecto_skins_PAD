package cl.skinspad.pago.domain;

import cl.skinspad.pago.escrow.Operacion;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

// Registro de un cobro con su comprador, método, monto, estado y skins incluidas.
@Entity
@Table(name = "pagos", uniqueConstraints = @jakarta.persistence.UniqueConstraint(columnNames="pedidoId"))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Boolean custodia;
    private Boolean simulado;
    private String etapaCustodia;
    @Column(length=500) private String errorCustodia;

    @JsonIgnore
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @Setter(AccessLevel.NONE)
    @org.hibernate.annotations.BatchSize(size=100)
    @OneToMany(mappedBy = "pago", cascade = CascadeType.PERSIST, fetch = FetchType.EAGER)
    @OrderBy("id")
    private List<Operacion> operaciones = new ArrayList<>();

    public Pago(Long id,String pedidoId,String usuarioId,BigDecimal montoTotal,MetodoPago metodo,EstadoPago estado,String referenciaExterna,LocalDateTime fecha){
        this.id=id;this.pedidoId=pedidoId;this.usuarioId=usuarioId;this.montoTotal=montoTotal;this.metodo=metodo;this.estado=estado;this.referenciaExterna=referenciaExterna;this.fecha=fecha;
    }

    @Column(nullable = false, length = 80)
    private String pedidoId;

    @Column(nullable = false, length = 80)
    private String usuarioId;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal montoTotal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MetodoPago metodo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoPago estado = EstadoPago.PENDIENTE;

    @Column(length = 200)
    private String referenciaExterna;
    private Integer cuotas;
    // Marca que se completaron los abonos a los vendedores y se solicitó el envío del recibo.
    private Boolean liquidado;
    private String numeroOperacion;

    @Column(nullable = false)
    private LocalDateTime fecha;

    public void agregarOperacion(Operacion operacion) {
        operacion.setPago(this);
        operaciones.add(operacion);
    }

    // Se conserva el formato de las respuestas de pagos a partir de la lista de operaciones.
    public List<Long> getProductoIds() {
        return operaciones.stream().map(Operacion::getProductoId).toList();
    }

    public Map<Long, BigDecimal> getPrecios() {
        Map<Long, BigDecimal> precios = new LinkedHashMap<>();
        for (var operacion : operaciones) {
            if (operacion.getPrecio() != null) {
                precios.put(operacion.getProductoId(), operacion.getPrecio());
            }
        }
        return precios;
    }
}
