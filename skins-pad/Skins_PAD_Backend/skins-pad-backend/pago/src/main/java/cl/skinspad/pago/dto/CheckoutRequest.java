package cl.skinspad.pago.dto;
import cl.skinspad.pago.domain.MetodoPago;
import jakarta.validation.constraints.*;
import java.util.List;
public record CheckoutRequest(@NotBlank @Size(max=50) String solicitudId,@NotEmpty @Size(max=100) List<@NotNull Long> productoIds,@NotNull MetodoPago metodo){}
