package cl.skinspad.usuario.controller;

import cl.skinspad.usuario.domain.Usuario;
import cl.skinspad.usuario.dto.*;
import cl.skinspad.usuario.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UsuarioService service;

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public Usuario registro(@Valid @RequestBody RegistroRequest r) {
        return service.registrar(r);
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest r) {
        return service.iniciarSesion(r);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@RequestHeader("Authorization") String authorization) {
        service.cerrarSesion(token(authorization));
    }

    @GetMapping("/validar")
    public Usuario validar(@RequestParam String token) {
        return service.validarToken(token);
    }

    private String token(String value) {
        return value.startsWith("Bearer ")?value.substring(7):value;
    }
}
