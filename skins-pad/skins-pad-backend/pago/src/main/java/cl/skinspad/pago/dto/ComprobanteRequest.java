package cl.skinspad.pago.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record ComprobanteRequest(@NotBlank @Size(max = 100) String banco, @NotBlank @Size(max = 120) String numeroOperacion, @NotBlank @Size(max = 1000) String archivoUrl) {
}
