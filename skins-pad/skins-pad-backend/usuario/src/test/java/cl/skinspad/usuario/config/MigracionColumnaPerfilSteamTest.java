package cl.skinspad.usuario.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class MigracionColumnaPerfilSteamTest {
    @Test
    void eliminaElEnlaceYConservaLaCuentaAlRepetirse() {
        var jdbc = new JdbcTemplate(new DriverManagerDataSource(
                "jdbc:h2:mem:usuario_legado;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", ""));
        jdbc.execute("CREATE TABLE usuarios (id VARCHAR(40) PRIMARY KEY, nombre VARCHAR(120), link_steam VARCHAR(500))");
        jdbc.update("INSERT INTO usuarios VALUES ('u1', 'Camila', 'https://steamcommunity.com/id/legado')");
        var migracion = new MigracionColumnaPerfilSteam(jdbc);
        var argumentos = new DefaultApplicationArguments();
        migracion.run(argumentos);
        migracion.run(argumentos);

        assertEquals("Camila", jdbc.queryForObject("SELECT nombre FROM usuarios WHERE id = 'u1'", String.class));
        assertEquals(0, jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = 'PUBLIC' AND table_name = 'USUARIOS' AND column_name = 'LINK_STEAM'
                """, Integer.class));
    }
}
