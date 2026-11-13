package cl.skinspad.usuario.dto;

import cl.skinspad.usuario.domain.Rol;
public record EstadoUsuarioRequest(Boolean suspendido, Boolean verificado, Rol rol, String motivo) {
}
