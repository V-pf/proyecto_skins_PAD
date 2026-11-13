package cl.skinspad.pago.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "comprobantes_transferencia")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComprobanteTransferencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long pagoId;

    @NotBlank
    @Column(nullable = false, length = 100)
    private String banco;

    @NotBlank
    @Column(nullable = false, length = 120)
    private String numeroOperacion;

    @NotBlank
    @Column(nullable = false, length = 1000)
    private String archivoUrl;

    @Column(nullable = false)
    private boolean verificado = false;

    @Column(length = 1000)
    private String verificadoUrl;
}
