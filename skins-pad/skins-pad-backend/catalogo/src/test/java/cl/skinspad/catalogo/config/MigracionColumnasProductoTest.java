package cl.skinspad.catalogo.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class MigracionColumnasProductoTest {
    @Test
    void eliminaLasColumnasYConservaLosAnunciosYReservasAlRepetirse() {
        var jdbc = new JdbcTemplate(new DriverManagerDataSource(
                "jdbc:h2:mem:productos_legados;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", ""));
        jdbc.execute("""
            CREATE TABLE productos (
                id BIGINT PRIMARY KEY,
                estado VARCHAR(32),
                reserva_pago VARCHAR(80),
                item_inventario_id BIGINT,
                motivo_rechazo VARCHAR(500),
                demo BOOLEAN,
                retirada_para_inventario BOOLEAN,
                traslado_pendiente BOOLEAN,
                url_intercambio VARCHAR(500)
            )
            """);
        jdbc.update("""
            INSERT INTO productos VALUES
                (1, 'INACTIVO', 'reserva-trasladada', 41, NULL, TRUE, TRUE, FALSE, NULL),
                (2, 'INACTIVO', 'reserva-normal', 42, 'El ítem dejó de estar disponible en Steam.', FALSE, FALSE, FALSE, NULL),
                (3, 'VENDIDO', 'venta-completada', 43, NULL, TRUE, TRUE, FALSE, NULL)
            """);
        jdbc.execute("CREATE TABLE imagenes_producto (id BIGINT PRIMARY KEY, url_imagen VARCHAR(255))");
        jdbc.update("""
            INSERT INTO imagenes_producto VALUES
              (1, 'https://community.fastly.steamstatic.com/economy/image/legada'),
              (2, '/img/arma-rifle.svg')
            """);

        var migracion = new MigracionColumnasProducto(jdbc);
        var args = new DefaultApplicationArguments(new String[0]);
        migracion.run(args);
        migracion.run(args);

        assertEquals(Set.of("id", "estado", "reserva_pago", "item_inventario_id", "motivo_rechazo"), Set.copyOf(
                jdbc.queryForList("""
                    SELECT LOWER(column_name) FROM information_schema.columns
                    WHERE table_schema = 'PUBLIC' AND table_name = 'PRODUCTOS'
                    """, String.class)));
        assertEquals(List.of("ELIMINADO", "INACTIVO", "VENDIDO"),
                jdbc.queryForList("SELECT estado FROM productos ORDER BY id", String.class));
        assertEquals(List.of("reserva-trasladada", "reserva-normal", "venta-completada"),
                jdbc.queryForList("SELECT reserva_pago FROM productos ORDER BY id", String.class));
        assertEquals(List.of(41L, 42L, 43L), jdbc.queryForList(
                "SELECT item_inventario_id FROM productos ORDER BY id", Long.class));
        assertEquals("El ítem ya no está disponible en el inventario.",
                jdbc.queryForObject("SELECT motivo_rechazo FROM productos WHERE id = 2", String.class));
        assertEquals(List.of("/img/arma-rifle.svg"),
                jdbc.queryForList("SELECT url_imagen FROM imagenes_producto", String.class));
    }
}
