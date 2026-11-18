package cl.skinspad.catalogo.dto;

import cl.skinspad.catalogo.domain.EstadoProducto;
import jakarta.validation.constraints.NotNull;
public record EstadoRequest(@NotNull EstadoProducto estado,
    @jakarta.validation.constraints.Size(max = 500) String motivoRechazo) {
    public EstadoRequest(EstadoProducto estado) { this(estado, null); }
}
