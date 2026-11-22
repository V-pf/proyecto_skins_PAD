package cl.skinspad.catalogo.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record PublicarDesdeInventarioRequest(
    @NotNull @Positive Long itemId,
    @NotNull @DecimalMin("1") @DecimalMax("50000000") @Digits(integer = 8, fraction = 0)
        BigDecimal precio) {}
