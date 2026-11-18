package cl.skinspad.catalogo.dto;
import jakarta.validation.constraints.*;
import java.util.List;
// Indica qué operación debe hacer el catálogo con las skins de una compra.
public record ReservaRequest(@NotBlank String referencia,@NotEmpty List<@NotNull Long> ids,@NotBlank String accion) {}
