package cl.skinspad.usuario.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
public record MovimientoSaldoRequest(@NotNull @DecimalMin("0.01") BigDecimal monto, String referencia) {
}
