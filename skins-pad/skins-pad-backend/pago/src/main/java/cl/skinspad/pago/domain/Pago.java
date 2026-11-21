package cl.skinspad.pago.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Registro de un cobro con su comprador, método, monto, estado y skins incluidas.
@Entity
@Table(name = "pagos", uniqueConstraints =
    @UniqueConstraint(name = "uk_pagos_pedido_id", columnNames = "pedidoId"))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @org.hibernate.annotations.BatchSize(size=100)
    @jakarta.persistence.ElementCollection(fetch=jakarta.persistence.FetchType.EAGER)
    private java.util.List<Long> productoIds=new java.util.ArrayList<>();
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
    // Guarda el precio de cada skin al iniciar la compra para poder consultarlo después.
    @org.hibernate.annotations.BatchSize(size=100)
    @jakarta.persistence.ElementCollection(fetch=jakarta.persistence.FetchType.EAGER)
    @jakarta.persistence.MapKeyColumn(name="producto_id")
    @Column(name="precio_venta",precision=15,scale=2)
    private java.util.Map<Long,BigDecimal> precios=new java.util.HashMap<>();
    private String numeroOperacion;

    @Column(nullable = false)
    private LocalDateTime fecha;
}
