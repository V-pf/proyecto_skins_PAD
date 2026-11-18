package cl.skinspad.catalogo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record CategoriaRequest(@NotBlank @Size(max = 80) String nombre, @Size(max = 500) String descripcion) {
}
