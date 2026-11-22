package cl.skinspad.pago.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import cl.skinspad.pago.escrow.*;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.time.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class HistorialPreciosServiceTest {
  @Test
  void adaptaPublicacionesAnterioresSinConfundirDesgasteOVariante() {
    assertEquals(
        "StatTrak™ AK-47 | Redline (Field-Tested)",
        HistorialPreciosService.nombreMercado(
            new HistorialPreciosService.Producto(1L, "AK-47 | Redline", false, "ft", true)));
    assertEquals(
        "AK-47 | Redline (Field-Tested)",
        HistorialPreciosService.nombreMercado(
            new HistorialPreciosService.Producto(
                1L, "AK-47 | Redline (Field-Tested)", false, "ft", false)));
  }

  @Test
  void agrupaPreciosVendidosYExcluyePruebasYOtrasVariantes() throws Exception {
    var servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    servidor.createContext(
        "/api/productos",
        exchange -> {
          byte[] json =
              ("[{\"id\":1,\"nombre\":\"AK-47 |"
                   + " Redline\",\"desgaste\":\"ft\",\"demo\":false,\"stattrak\":false},{\"id\":2,\"nombre\":\"AK-47"
                   + " | Redline\",\"desgaste\":\"ft\",\"demo\":true},{\"id\":3,\"nombre\":\"AK-47"
                   + " | Redline\",\"desgaste\":\"mw\",\"demo\":false}]")
                  .getBytes(java.nio.charset.StandardCharsets.UTF_8);
          exchange.getResponseHeaders().set("Content-Type", "application/json");
          exchange.sendResponseHeaders(200, json.length);
          exchange.getResponseBody().write(json);
          exchange.close();
        });
    servidor.start();
    try {
      var repo = mock(OperacionRepository.class);
      var service = new HistorialPreciosService(repo);
      ReflectionTestUtils.setField(
          service, "catalogo", "http://127.0.0.1:" + servidor.getAddress().getPort());
      var fecha =
          LocalDate.now(ZoneId.of("America/Santiago"))
              .minusDays(1)
              .atStartOfDay(ZoneId.of("America/Santiago"))
              .toInstant();
      var a = new Operacion();
      a.setProductoId(1L);
      a.setPrecio(1000);
      a.setCompletada(fecha);
      var b = new Operacion();
      b.setProductoId(1L);
      b.setPrecio(3000);
      b.setCompletada(fecha);
      var demo = new Operacion();
      demo.setProductoId(2L);
      demo.setPrecio(90000);
      demo.setCompletada(fecha);
      var otro = new Operacion();
      otro.setProductoId(3L);
      otro.setPrecio(90000);
      otro.setCompletada(fecha);
      when(repo.findByEstadoAndCompletadaGreaterThanEqual(eq(EstadoOperacion.COMPLETADA), any()))
          .thenReturn(List.of(a, b, demo, otro));
      var puntos = service.consultar("AK-47 | Redline (Field-Tested)", 30);
      assertEquals(1, puntos.size());
      assertEquals(2000, puntos.get(0).precioMedio());
      assertEquals(2, puntos.get(0).ventas());
      assertThrows(IllegalArgumentException.class, () -> service.consultar("AK-47", 500));
    } finally {
      servidor.stop(0);
    }
  }
}
