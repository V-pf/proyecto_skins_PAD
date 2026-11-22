package cl.skinspad.catalogo.dto;

import jakarta.validation.constraints.NotNull;
import java.util.Set;

public record DisponibilidadInventarioRequest(@NotNull Set<Long> disponibles) {}
