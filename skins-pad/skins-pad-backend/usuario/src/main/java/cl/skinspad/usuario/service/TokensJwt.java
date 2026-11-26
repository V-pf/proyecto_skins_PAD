package cl.skinspad.usuario.service;

import cl.skinspad.usuario.domain.Usuario;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.*;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.*;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class TokensJwt {
    private final RSAKey clave;
    private static final String EMISOR = cl.skinspad.seguridad.VerificadorJwt.EMISOR;
    private static final String AUDIENCIA = cl.skinspad.seguridad.VerificadorJwt.AUDIENCIA;

    public TokensJwt(@Value("${SKINS_JWT_PRIVATE_JWK:}") String jwk) throws Exception {
        clave = jwk.isBlank() ? new RSAKeyGenerator(2048).keyIDFromThumbprint(true).generate() : RSAKey.parse(jwk);
        if (!clave.isPrivate() || clave.size() < 2048) throw new IllegalArgumentException("Clave RSA privada inválida");
    }

    public Map<String, Object> clavesPublicas() {
        return Map.of("keys", List.of(clave.toPublicJWK().toJSONObject()));
    }

    public String emitir(Usuario usuario, String sesion) {
        Instant ahora = Instant.now();
        var claims = new JWTClaimsSet.Builder().issuer(EMISOR).audience(AUDIENCIA)
            .subject(usuario.getId()).claim("sid", sesion).claim("rol", usuario.getRol().name())
            .issueTime(Date.from(ahora)).expirationTime(Date.from(ahora.plusSeconds(900))).build();
        var jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(clave.getKeyID()).build(), claims);
        try {
            jwt.sign(new RSASSASigner(clave));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException("No se pudo firmar el token", e);
        }
    }

    public String sesion(String bearer) {
        try {
            if (bearer == null || !bearer.startsWith("Bearer ")) throw invalido();
            var jwt = SignedJWT.parse(bearer.substring(7));
            var c = jwt.getJWTClaimsSet();
            if (!JWSAlgorithm.RS256.equals(jwt.getHeader().getAlgorithm())
                || !jwt.verify(new RSASSAVerifier(clave.toPublicJWK()))
                || !EMISOR.equals(c.getIssuer()) || !List.of(AUDIENCIA).equals(c.getAudience())
                || c.getExpirationTime() == null || !c.getExpirationTime().after(new Date())
                || c.getStringClaim("sid") == null) throw invalido();
            return c.getStringClaim("sid");
        } catch (java.text.ParseException | JOSEException e) {
            throw invalido();
        }
    }

    public static ResponseStatusException invalido() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesión inválida o vencida");
    }
}
