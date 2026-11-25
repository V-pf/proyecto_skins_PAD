package cl.skinspad.seguridad;

import com.nimbusds.jose.*;
import com.nimbusds.jose.jwk.source.*;
import com.nimbusds.jose.proc.*;
import com.nimbusds.jwt.proc.*;
import java.net.URI;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** La clave pública se almacena en caché. El rol vigente se consulta por separado. */
public final class VerificadorJwt {
    public static final String EMISOR = "skins-pad/usuario";
    public static final String AUDIENCIA = "skins-pad/api";
    private final DefaultJWTProcessor<SecurityContext> processor = new DefaultJWTProcessor<>();

    public VerificadorJwt(String usuarioUrl) throws java.net.MalformedURLException {
        var keys = JWKSourceBuilder.<SecurityContext>create(URI.create(usuarioUrl + "/api/auth/jwks").toURL()).build();
        processor.setJWSKeySelector(new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, keys));
        var claims = new DefaultJWTClaimsVerifier<SecurityContext>(AUDIENCIA,
            new com.nimbusds.jwt.JWTClaimsSet.Builder().issuer(EMISOR).build(),
            Set.of("sub", "sid", "exp", "iat"));
        claims.setMaxClockSkew(0);
        processor.setJWTClaimsSetVerifier(claims);
    }

    public void verificar(String token) {
        try {
            processor.process(token, null);
        } catch (JOSEException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Clave pública no disponible", e);
        } catch (BadJOSEException | java.text.ParseException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "JWT inválido", e);
        }
    }
}
