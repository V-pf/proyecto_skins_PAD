package cl.skinspad.catalogo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CatalogoApplication {
    @org.springframework.context.annotation.Bean
    cl.skinspad.seguridad.VerificadorJwt verificadorJwt(
            @org.springframework.beans.factory.annotation.Value("${USUARIO_URL:http://localhost:8087}") String url)
            throws java.net.MalformedURLException {
        return new cl.skinspad.seguridad.VerificadorJwt(url);
    }

    public static void main(String[] args) {
        SpringApplication.run(CatalogoApplication.class, args);
    }
}
