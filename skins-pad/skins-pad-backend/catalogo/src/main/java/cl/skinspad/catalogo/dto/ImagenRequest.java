package cl.skinspad.catalogo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record ImagenRequest(@NotBlank @Size(max = 45000) String urlImagen, boolean esPrincipal) {
}
