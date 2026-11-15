package cl.skinspad.catalogo.dto;

import cl.skinspad.catalogo.domain.EstadoProducto;
import jakarta.validation.constraints.NotNull;
public record EstadoRequest(@NotNull EstadoProducto estado) {
}
