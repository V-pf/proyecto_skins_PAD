package cl.skinspad.pago.config;

import cl.skinspad.pago.escrow.Operacion;
import cl.skinspad.pago.repository.PagoRepository;
import java.math.BigDecimal;
import java.sql.ResultSetMetaData;
import java.sql.Types;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class MigracionDetallePago implements ApplicationRunner {
    private static final ZoneId ZONA = ZoneId.of("America/Santiago");
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transacciones;
    private final PagoRepository pagos;

    private record Detalle(Long pagoId, Long productoId, BigDecimal precio) {}

    @Override
    public void run(ApplicationArguments args) {
        prepararColumnaPrecio();
        retirarEnlaceDeIntercambio();
        Set<String> tablas = jdbc.execute((ConnectionCallback<Set<String>>) conexion -> {
            Set<String> nombres = new HashSet<>();
            try (var filas = conexion.getMetaData().getTables(conexion.getCatalog(), conexion.getSchema(), "%", null)) {
                while (filas.next()) nombres.add(filas.getString("TABLE_NAME").toLowerCase(Locale.ROOT));
            }
            return nombres;
        });
        boolean tieneProductos = tablas.contains("pago_producto_ids");
        boolean tienePrecios = tablas.contains("pago_precios");
        if (!tieneProductos && !tienePrecios) return;

        // MariaDB confirma el DDL por separado. Las tablas antiguas se retiran después del commit.
        transacciones.executeWithoutResult(estado -> migrar(tieneProductos, tienePrecios));
        jdbc.execute("DROP TABLE IF EXISTS pago_producto_ids, pago_precios");
    }

    private void retirarEnlaceDeIntercambio() {
        boolean tieneEnlace = jdbc.query("SELECT * FROM operaciones WHERE 1 = 0", filas -> {
            var columnas = filas.getMetaData();
            for (int i = 1; i <= columnas.getColumnCount(); i++) {
                if ("url_intercambio".equalsIgnoreCase(columnas.getColumnName(i))) return true;
            }
            return false;
        });
        if (tieneEnlace) jdbc.execute("ALTER TABLE operaciones DROP COLUMN url_intercambio");
    }

    private void prepararColumnaPrecio() {
        boolean compatible = jdbc.query("SELECT precio FROM operaciones WHERE 1 = 0", filas -> {
            var columna = filas.getMetaData();
            int tipo = columna.getColumnType(1);
            return (tipo == Types.NUMERIC || tipo == Types.DECIMAL)
                    && columna.getPrecision(1) >= 21 && columna.getScale(1) == 2
                    && columna.isNullable(1) == ResultSetMetaData.columnNullable;
        });
        if (compatible) return;

        String motor = jdbc.execute((ConnectionCallback<String>) conexion -> conexion.getMetaData().getDatabaseProductName());
        jdbc.execute("H2".equals(motor)
                ? "ALTER TABLE operaciones ALTER COLUMN precio DECIMAL(21,2) NULL"
                : "ALTER TABLE operaciones MODIFY COLUMN precio DECIMAL(21,2) NULL");
    }

    private void migrar(boolean tieneProductos, boolean tienePrecios) {
        String productos = "SELECT pago_id, producto_ids AS producto_id FROM pago_producto_ids WHERE producto_ids IS NOT NULL";
        String precios = "SELECT pago_id, producto_id FROM pago_precios";
        String referencias = tieneProductos && tienePrecios ? productos + " UNION " + precios
                : tieneProductos ? productos : precios;
        String consulta = "SELECT DISTINCT r.pago_id, r.producto_id, "
                + (tienePrecios ? "p.precio_venta" : "NULL AS precio_venta") + " FROM (" + referencias + ") r"
                + (tienePrecios ? " LEFT JOIN pago_precios p ON p.pago_id = r.pago_id AND p.producto_id = r.producto_id" : "")
                + " ORDER BY r.pago_id, r.producto_id";
        var detalles = jdbc.query(consulta, (fila, numero) -> new Detalle(
                fila.getLong("pago_id"), fila.getLong("producto_id"), fila.getBigDecimal("precio_venta")));
        detalles.stream().collect(Collectors.groupingBy(Detalle::pagoId, LinkedHashMap::new, Collectors.toList()))
                .forEach((pagoId, lineas) -> consolidar(pagoId, lineas, tienePrecios));
    }

    private void consolidar(Long pagoId, List<Detalle> lineas, boolean tienePrecios) {
        var pago = pagos.bloquear(pagoId).orElseThrow(() ->
                new IllegalStateException("No existe el pago " + pagoId + " para migrar sus productos"));
        boolean enCustodia = Boolean.TRUE.equals(pago.getCustodia());
        var actuales = pago.getOperaciones().stream()
                .collect(Collectors.toMap(Operacion::getProductoId, Function.identity()));
        for (var linea : lineas) {
            var operacion = actuales.get(linea.productoId());
            if (operacion == null) {
                if (enCustodia) throw new IllegalStateException("Falta una operación de custodia para el pago " + pagoId);
                pago.agregarOperacion(Operacion.pendienteDeImportar(linea.productoId(), pago.getUsuarioId(),
                        linea.precio(), pago.getFecha().atZone(ZONA).toInstant()));
            } else if (enCustodia || !operacion.isHistorica()) {
                if (linea.precio() != null && (operacion.getPrecio() == null
                        || linea.precio().compareTo(operacion.getPrecio()) != 0)) {
                    throw new IllegalStateException("El precio histórico no coincide con la operación " + operacion.getId());
                }
            } else if (linea.precio() != null || (tienePrecios && operacion.getPrecio() != null
                    && operacion.getPrecio().signum() == 0)) {
                operacion.setPrecio(linea.precio());
            }
        }
    }
}
