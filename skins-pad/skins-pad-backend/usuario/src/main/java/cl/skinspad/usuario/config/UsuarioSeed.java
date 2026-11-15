package cl.skinspad.usuario.config;

import cl.skinspad.usuario.domain.*;
import cl.skinspad.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.math.BigDecimal;
import java.time.LocalDate;

@Configuration
@RequiredArgsConstructor
public class UsuarioSeed {

    @Bean @org.springframework.core.annotation.Order(0) CommandLineRunner cargarUsuarios(UsuarioRepository repo, PasswordEncoder encoder) {
        return args -> {
            if(repo.count()>0) return;
            repo.save(usuario("u1", "Camila Torres", "penquista_gamer", "camila@correo.com", "Demo1234!", "2000-05-14", Rol.cliente, false, false, "12500", encoder));
            repo.save(usuario("u2", "Administrador Skins PAD", "admin", "admin@skinspad.example", "Admin1234!", "1995-01-01", Rol.admin, false, true, "0", encoder));
            repo.save(usuario("u3", "Penquista Skins", "PenquistaSkins", "vendedor1@correo.com", "Demo1234!", "1998-03-02", Rol.cliente, false, true, "0", encoder));
            repo.save(usuario("u4", "Bio Bio Skins", "BioBioSkins", "vendedor2@correo.com", "Demo1234!", "1997-08-21", Rol.cliente, false, true, "0", encoder));
            repo.save(usuario("u6", "Hualpén Trade", "HualpenTrade", "hualpen@skinspad.example", "Demo1234!", "1998-01-01", Rol.cliente, false, true, "0", encoder));
            repo.save(usuario("u7", "Lota Loot", "LotaLoot", "lota@skinspad.example", "Demo1234!", "1998-01-01", Rol.cliente, false, true, "0", encoder));
            repo.save(usuario("u8", "Coronel Drops", "CoronelDrops", "coronel@skinspad.example", "Demo1234!", "1998-01-01", Rol.cliente, false, true, "0", encoder));
            repo.save(usuario("u5", "Usuario Reportado", "usuario_reportado22", "reportado@correo.com", "Demo1234!", "1999-11-30", Rol.cliente, true, false, "0", encoder));
        };
    }

    @Bean
    @org.springframework.context.annotation.Profile("demo")
    @org.springframework.core.annotation.Order(10)
    CommandLineRunner cargarVendedorDemo(UsuarioRepository repo, PasswordEncoder encoder) {
        return args -> {
            // Añade la cuenta a bases demo existentes sin restablecer contraseña, saldo ni moderación.
            if (!repo.existsByUsernameIgnoreCase("vendedor_demo")
                && !repo.existsByEmailIgnoreCase("vendedor_demo@skinspad.example")
                && !repo.existsById("demo-vendedor")) {
                repo.save(usuario("demo-vendedor", "Vendedor de demostración", "vendedor_demo",
                    "vendedor_demo@skinspad.example", "Demo1234!", "1998-01-01", Rol.cliente,
                    false, true, "0", encoder));
            }
        };
    }

    private Usuario usuario(String id, String nombre, String username, String email, String pass, String fecha, Rol rol, boolean suspendido, boolean verificado, String saldo, PasswordEncoder encoder) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setNombre(nombre);
        u.setUsername(username);
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(pass));
        u.setFechaNacimiento(LocalDate.parse(fecha));
        u.setLinkSteam("");
        u.setRol(rol);
        u.setSuspendido(suspendido);
        u.setVerificado(verificado);
        u.setSaldo(new BigDecimal(saldo));
        return u;
    }
}
