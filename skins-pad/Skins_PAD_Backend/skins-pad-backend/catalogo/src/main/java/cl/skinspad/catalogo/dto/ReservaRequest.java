package cl.skinspad.catalogo.dto;
import jakarta.validation.constraints.*;
import java.util.List;
public record ReservaRequest(@NotBlank String referencia,@NotEmpty List<@NotNull Long> ids,@NotBlank String accion) {}
