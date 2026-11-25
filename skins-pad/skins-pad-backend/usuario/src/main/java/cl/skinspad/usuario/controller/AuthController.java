package cl.skinspad.usuario.controller;

import cl.skinspad.usuario.domain.Usuario;
import cl.skinspad.usuario.dto.*;
import cl.skinspad.usuario.service.*;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import java.util.Arrays;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UsuarioService service;
    private final TokensJwt jwt;
    @org.springframework.beans.factory.annotation.Value("${SKINS_COOKIE_SECURE:false}")
    private boolean cookieSegura;
    private static final String COOKIE = "skins_refresh_";

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public Usuario registro(@Valid @RequestBody RegistroRequest r) {
        return service.registrar(r);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest r,
            @RequestParam(defaultValue = "false") boolean recordar, HttpServletRequest request) {
        var login = service.iniciarSesion(r, recordar);
        String sid = login.sesion().getToken();
        var cookie = cookie(sid, login.token(), request);
        if (recordar) cookie.maxAge(java.time.Duration.ofDays(30));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
            .header(HttpHeaders.SET_COOKIE, cookie.build().toString())
            .body(Map.of("usuario", login.usuario(), "token", jwt.emitir(login.usuario(), sid)));
    }

    private ResponseCookie.ResponseCookieBuilder cookie(String sid, String valor, HttpServletRequest request) {
        return ResponseCookie.from(COOKIE + sid, valor).httpOnly(true).secure(cookieSegura || request.isSecure())
            .sameSite("Strict").path("/api/auth");
    }

    @PostMapping("/renovar")
    public ResponseEntity<?> renovar(@RequestHeader("X-Skins-Sesion") String sid, HttpServletRequest request) {
        if (!sid.matches("[a-f0-9]{64}") || request.getCookies() == null) throw TokensJwt.invalido();
        var secreto = Arrays.stream(request.getCookies()).filter(c -> (COOKIE + sid).equals(c.getName()))
            .map(Cookie::getValue).findFirst().orElseThrow(TokensJwt::invalido);
        if (!CredencialRenovacion.idSesion(secreto).equals(sid)) throw TokensJwt.invalido();
        var usuario = service.renovarSesion(sid);
        var respuesta = ResponseEntity.ok().cacheControl(CacheControl.noStore());
        if (secreto.startsWith("p.")) respuesta.header(HttpHeaders.SET_COOKIE,
            cookie(sid, secreto, request).maxAge(java.time.Duration.ofDays(30)).build().toString());
        return respuesta.body(Map.of("token", jwt.emitir(usuario, sid)));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authorization, HttpServletRequest request) {
        String sid = jwt.sesion(authorization);
        service.cerrarSesion(sid);
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE,
            cookie(sid, "", request).maxAge(0).build().toString()).build();
    }

    @GetMapping("/validar")
    public Usuario validar(@RequestHeader(value="Authorization", required=false) String bearer) {
        var usuario = vigente(jwt.sesion(bearer));
        if (usuario.isSuspendido()) throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN);
        return usuario;
    }

    private Usuario vigente(String sid) {
        try {
            return service.usuarioDeSesion(sid);
        } catch (IllegalArgumentException | jakarta.persistence.EntityNotFoundException e) {
            throw TokensJwt.invalido();
        }
    }

    @GetMapping("/jwks")
    public Map<String, Object> jwks() {
        return jwt.clavesPublicas();
    }
}
