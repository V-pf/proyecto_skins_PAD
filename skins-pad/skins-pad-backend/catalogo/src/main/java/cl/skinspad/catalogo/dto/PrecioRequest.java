package cl.skinspad.catalogo.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public record PrecioRequest(@NotNull @DecimalMin("1.00") @DecimalMax("50000000.00") BigDecimal nuevoPrecio) {
}
