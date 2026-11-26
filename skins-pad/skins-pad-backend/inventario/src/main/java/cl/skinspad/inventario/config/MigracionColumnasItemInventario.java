package cl.skinspad.inventario.config;

import java.util.HashSet;
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
public class MigracionColumnasItemInventario implements ApplicationRunner {
  private final JdbcTemplate jdbc;

  @Override
  public void run(ApplicationArguments args) {
    Set<String> columnas = jdbc.query("SELECT * FROM item_inventario WHERE 1 = 0", resultado -> {
      var metadatos = resultado.getMetaData();
      Set<String> nombres = new HashSet<>();
      for (int i = 1; i <= metadatos.getColumnCount(); i++) {
        nombres.add(metadatos.getColumnName(i).toLowerCase(Locale.ROOT));
      }
      return nombres;
    });

    boolean tieneOrigen = columnas.contains("producto_origen_id");
    if (tieneOrigen) {
      // El sufijo conserva la unicidad cuando ya existe el asset canónico del mismo anuncio.
      jdbc.update("""
          UPDATE item_inventario
          SET asset_id = CONCAT('catalogo-', producto_origen_id, '-', id)
          WHERE producto_origen_id IS NOT NULL
            AND asset_id <> CONCAT('catalogo-', producto_origen_id)
            AND asset_id <> CONCAT('catalogo-', producto_origen_id, '-', id)
          """);
    }

    if (columnas.contains("demo")) {
      String sinOrigen = tieneOrigen ? "producto_origen_id IS NULL" : "asset_id NOT LIKE 'catalogo-%'";
      jdbc.update("""
          UPDATE item_inventario
          SET asset_id = CONCAT('precarga-', id)
          WHERE demo = TRUE AND asset_id <> CONCAT('precarga-', id)
            AND %s
          """.formatted(sinOrigen));
      jdbc.execute("ALTER TABLE item_inventario DROP COLUMN demo");
    }

    if (tieneOrigen) {
      jdbc.execute("ALTER TABLE item_inventario DROP COLUMN producto_origen_id");
    }

    if (columnas.contains("imagen")) {
      jdbc.update("""
          UPDATE item_inventario SET imagen = NULL
          WHERE LOWER(imagen) LIKE 'https://community.fastly.steamstatic.com/economy/image/%'
          """);
    }
    jdbc.execute("DROP TABLE IF EXISTS sincronizacion_inventario");
  }
}
