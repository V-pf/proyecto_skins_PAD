package cl.skinspad.catalogo.dto;

import cl.skinspad.catalogo.domain.EstadoProducto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public record ProductoRequest(String vendedorId, String vendedor, @NotNull Long categoriaId,
    @NotBlank @Size(max = 160) String nombre, @Size(max = 1500) String descripcion,
    @NotNull @DecimalMin("1.00") @DecimalMax("50000000.00") BigDecimal precio,
    EstadoProducto estado, String tipo, String grado, String desgaste, Boolean stattrak,
    @DecimalMin("0.0") @DecimalMax("1.0") BigDecimal floatValor, @Size(max=500) String motivoRechazo) {
}
