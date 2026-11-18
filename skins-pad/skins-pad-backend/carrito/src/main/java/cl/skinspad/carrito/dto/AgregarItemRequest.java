package cl.skinspad.carrito.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
public record AgregarItemRequest(@NotNull @Positive Long productoId) {
}
