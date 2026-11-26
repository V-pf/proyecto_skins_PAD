import java.sql.*;
import java.util.Objects;

/** Ejecutar una vez, con los servicios detenidos y los esquemas de destino ya creados. */
public class MigrarInventario {
  private static final String[] CAMPOS_ITEM = {
      "id", "usuario_id", "asset_id", "nombre", "tipo", "grado", "desgaste",
      "stattrak", "imagen", "intercambiable", "presente"
  };

  public static void main(String[] args) throws SQLException {
    if (args.length != 0) throw new IllegalArgumentException("Configura los orígenes mediante variables de entorno.");
    try (Connection origen = conectar("CATALOGO");
         Connection inventario = conectar("INVENTARIO")) {
      inventario.setAutoCommit(false);
      try {
        migrarItems(origen, inventario);
        inventario.commit();
      } catch (SQLException | RuntimeException error) {
        inventario.rollback();
        throw error;
      }
      ajustarIdentidadH2(inventario);
      System.out.println("Migración completada. El catálogo original quedó intacto.");
    }
  }

  private static Connection conectar(String servicio) throws SQLException {
    String clave = servicio + "_DB_";
    String url = System.getenv(clave + "URL");
    if (url == null || url.isBlank()) throw new IllegalArgumentException("Falta " + clave + "URL.");
    String usuario = System.getenv().getOrDefault(clave + "USERNAME", url.startsWith("jdbc:h2:") ? "sa" : "root");
    String contrasena = System.getenv().getOrDefault(clave + "PASSWORD", "");
    return DriverManager.getConnection(url, usuario, contrasena);
  }

  private static void migrarItems(Connection origen, Connection destino) throws SQLException {
    String campos = String.join(", ", CAMPOS_ITEM);
    String camposOrigen = campos.replace("asset_id",
        "case when demo = true and asset_id not like 'catalogo-%' "
        + "then concat('precarga-', id) else asset_id end as asset_id");
    try (var consulta = origen.createStatement();
         var filas = consulta.executeQuery("select " + camposOrigen + " from item_inventario order by id");
         var existente = destino.prepareStatement("select usuario_id, asset_id from item_inventario where id = ?");
         var insertar = destino.prepareStatement(
             "insert into item_inventario (" + campos + ") values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
      while (filas.next()) {
        long id = filas.getLong("id");
        existente.setLong(1, id);
        try (var actual = existente.executeQuery()) {
          if (actual.next()) {
            if (!Objects.equals(actual.getString("usuario_id"), filas.getString("usuario_id"))
                || !Objects.equals(actual.getString("asset_id"), filas.getString("asset_id"))) {
              throw new IllegalStateException("El ID " + id + " ya se usa para otro ítem. Usa un destino vacío.");
            }
            continue;
          }
        }
        for (int n = 0; n < CAMPOS_ITEM.length; n++) {
          insertar.setObject(n + 1, filas.getObject(CAMPOS_ITEM[n]));
        }
        insertar.executeUpdate();
      }
    }
  }

  private static void ajustarIdentidadH2(Connection destino) throws SQLException {
    if (!destino.getMetaData().getDatabaseProductName().equals("H2")) return;
    long siguiente;
    try (var consulta = destino.createStatement();
         var maximo = consulta.executeQuery("select coalesce(max(id), 0) + 1 from item_inventario")) {
      maximo.next();
      siguiente = maximo.getLong(1);
    }
    try (var ddl = destino.createStatement()) {
      ddl.executeUpdate("alter table item_inventario alter column id restart with " + siguiente);
    }
  }
}
