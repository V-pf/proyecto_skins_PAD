package cl.skinspad.usuario.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
// El monto y la referencia que permiten registrar un cambio de saldo una sola vez.
public record MovimientoSaldoRequest(@NotNull @DecimalMin("0.01") BigDecimal monto, String referencia) {
}
