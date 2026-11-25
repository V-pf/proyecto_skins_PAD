package cl.skinspad.pago;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
@org.springframework.scheduling.annotation.EnableScheduling
public class PagoApplication {
    @org.springframework.context.annotation.Bean
    cl.skinspad.seguridad.VerificadorJwt verificadorJwt(
            @org.springframework.beans.factory.annotation.Value("${USUARIO_URL:http://localhost:8087}") String url)
            throws java.net.MalformedURLException {
        return new cl.skinspad.seguridad.VerificadorJwt(url);
    }

    public static void main(String[] args) {
        SpringApplication.run(PagoApplication.class, args);
    }
}
