package cl.skinspad.usuario.dto;
import jakarta.validation.constraints.*;
public record PerfilRequest(@NotBlank @Size(max=120) String nombre,@NotBlank @Email @Size(max=160) String email){}
