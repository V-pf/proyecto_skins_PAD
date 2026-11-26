package cl.skinspad.inventario.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class MigracionColumnasItemInventarioTest {
  @Test
  void eliminaLasColumnasYConservaLosIdsDeInventarioAlRepetirse() {
    var jdbc = new JdbcTemplate(new DriverManagerDataSource(
        "jdbc:h2:mem:inventario_legado;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", ""));
    jdbc.execute("""
        CREATE TABLE item_inventario (
          id BIGINT PRIMARY KEY,
          usuario_id VARCHAR(80) NOT NULL,
          asset_id VARCHAR(255) NOT NULL,
          presente BOOLEAN,
          demo BOOLEAN,
          producto_origen_id BIGINT,
          imagen VARCHAR(255),
          UNIQUE (usuario_id, asset_id)
        )
        """);
    jdbc.update("""
        INSERT INTO item_inventario VALUES
          (1, 'legado', '123456789', FALSE, FALSE, NULL, 'https://community.fastly.steamstatic.com/economy/image/legada'),
          (2, 'dueño', 'catalogo-42', TRUE, TRUE, 42, '/img/arma-rifle.svg'),
          (3, 'dueño', 'catalogo-antiguo', TRUE, TRUE, 42, NULL),
          (4, 'dueño', 'demo-antiguo', TRUE, TRUE, NULL, NULL)
        """);
    jdbc.execute("CREATE TABLE sincronizacion_inventario (usuario_id VARCHAR(80) PRIMARY KEY)");

    var migracion = new MigracionColumnasItemInventario(jdbc);
    var args = new DefaultApplicationArguments();
    migracion.run(args);
    migracion.run(args);

    assertEquals(Set.of("id", "usuario_id", "asset_id", "presente", "imagen"), Set.copyOf(
        jdbc.queryForList("""
            SELECT LOWER(column_name) FROM information_schema.columns
            WHERE table_schema = 'PUBLIC' AND table_name = 'ITEM_INVENTARIO'
            """, String.class)));
    assertEquals(List.of(1L, 2L, 3L, 4L),
        jdbc.queryForList("SELECT id FROM item_inventario ORDER BY id", Long.class));
    assertEquals(List.of("123456789", "catalogo-42", "catalogo-42-3", "precarga-4"),
        jdbc.queryForList("SELECT asset_id FROM item_inventario ORDER BY id", String.class));
    assertEquals(List.of(false, true, true, true),
        jdbc.queryForList("SELECT presente FROM item_inventario ORDER BY id", Boolean.class));
    assertNull(jdbc.queryForObject("SELECT imagen FROM item_inventario WHERE id = 1", String.class));
    assertEquals("/img/arma-rifle.svg",
        jdbc.queryForObject("SELECT imagen FROM item_inventario WHERE id = 2", String.class));
    assertEquals(0, jdbc.queryForObject("""
        SELECT COUNT(*) FROM information_schema.tables
        WHERE table_schema = 'PUBLIC' AND table_name = 'SINCRONIZACION_INVENTARIO'
        """, Integer.class));
  }
}
