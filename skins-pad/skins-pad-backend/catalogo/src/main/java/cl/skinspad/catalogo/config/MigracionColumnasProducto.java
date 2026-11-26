package cl.skinspad.catalogo.config;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
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
public class MigracionColumnasProducto implements ApplicationRunner {
    private static final List<String> COLUMNAS_RETIRADAS =
            List.of("demo", "retirada_para_inventario", "traslado_pendiente", "url_intercambio");

    private final JdbcTemplate jdbc;

    @Override
    public void run(ApplicationArguments args) {
        Set<String> columnas = jdbc.query("SELECT * FROM productos WHERE 1 = 0", resultado -> {
            var metadatos = resultado.getMetaData();
            Set<String> nombres = new HashSet<>();
            for (int i = 1; i <= metadatos.getColumnCount(); i++) {
                nombres.add(metadatos.getColumnName(i).toLowerCase(Locale.ROOT));
            }
            return nombres;
        });

        if (columnas.contains("retirada_para_inventario")) {
            // La referencia conserva la reserva y el estado determina si el anuncio vuelve al catálogo.
            jdbc.update("""
                UPDATE productos SET estado = 'ELIMINADO'
                WHERE retirada_para_inventario = TRUE
                  AND reserva_pago IS NOT NULL AND estado = 'INACTIVO'
                """);
        }

        for (String columna : COLUMNAS_RETIRADAS) {
            if (columnas.contains(columna)) {
                jdbc.execute("ALTER TABLE productos DROP COLUMN " + columna);
            }
        }

        if (columnas.contains("motivo_rechazo")) {
            jdbc.update("UPDATE productos SET motivo_rechazo = ? WHERE motivo_rechazo = ?",
                    "El ítem ya no está disponible en el inventario.", "El ítem dejó de estar disponible en Steam.");
        }
        jdbc.update("""
            DELETE FROM imagenes_producto
            WHERE LOWER(url_imagen) LIKE 'https://community.fastly.steamstatic.com/economy/image/%'
            """);
    }
}
