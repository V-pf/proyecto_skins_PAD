package cl.skinspad.usuario.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;

// Registra un cambio de saldo con una referencia única para esa cuenta.
@Entity
@Getter
@NoArgsConstructor
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"cuenta", "referencia"}))
public class Movimiento {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String cuenta;

  @Column(nullable = false)
  private String referencia;

  private BigDecimal monto;
  private String tipo;
  private LocalDateTime fecha;

  public Movimiento(String cuenta, String referencia, BigDecimal monto, String tipo) {
    this.cuenta = cuenta;
    this.referencia = referencia;
    this.monto = monto;
    this.tipo = tipo;
    this.fecha = LocalDateTime.now();
  }
}
