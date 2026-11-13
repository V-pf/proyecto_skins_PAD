package cl.skinspad.usuario;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@org.springframework.scheduling.annotation.EnableScheduling
@SpringBootApplication
public class UsuarioApplication {
    public static void main(String[] args) {
        SpringApplication.run(UsuarioApplication.class, args);
    }
}
