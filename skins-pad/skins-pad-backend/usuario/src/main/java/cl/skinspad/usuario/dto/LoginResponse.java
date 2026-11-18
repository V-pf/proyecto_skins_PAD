package cl.skinspad.usuario.dto;

import cl.skinspad.usuario.domain.Sesion;
import cl.skinspad.usuario.domain.Usuario;
public record LoginResponse(String token, Usuario usuario, Sesion sesion) {
}
