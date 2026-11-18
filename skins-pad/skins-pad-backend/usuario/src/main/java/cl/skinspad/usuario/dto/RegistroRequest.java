package cl.skinspad.usuario.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
public record RegistroRequest(@NotBlank @Size(max = 120) String nombre,
    @NotBlank @Size(min = 3, max = 60) String username, @NotBlank @Email @Size(max = 160) String email,
    @NotBlank @Size(min = 8, max = 72) String password, @NotNull @Past LocalDate fechaNacimiento,
    @Size(max = 500) String linkSteam) {
}
