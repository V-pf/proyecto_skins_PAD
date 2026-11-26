package cl.skinspad.usuario.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class MigracionColumnaPerfilSteam implements ApplicationRunner {
    private final JdbcTemplate jdbc;

    @Override
    public void run(ApplicationArguments args) {
        boolean tieneEnlace = jdbc.query("SELECT * FROM usuarios WHERE 1 = 0", filas -> {
            var columnas = filas.getMetaData();
            for (int i = 1; i <= columnas.getColumnCount(); i++) {
                if ("link_steam".equalsIgnoreCase(columnas.getColumnName(i))) return true;
            }
            return false;
        });
        if (tieneEnlace) jdbc.execute("ALTER TABLE usuarios DROP COLUMN link_steam");
    }
}
