package cl.skinspad.pago.dto;

import cl.skinspad.pago.domain.MetodoPago;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public record IniciarPagoRequest(@NotBlank String pedidoId, @NotBlank String usuarioId,
    @NotNull @DecimalMin("0.01") BigDecimal montoTotal, @NotNull MetodoPago metodo, String referenciaExterna) {
}
