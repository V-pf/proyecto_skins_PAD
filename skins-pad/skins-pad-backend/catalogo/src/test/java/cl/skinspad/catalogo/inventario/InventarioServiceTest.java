package cl.skinspad.catalogo.inventario;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.skinspad.catalogo.domain.EstadoProducto;
import cl.skinspad.catalogo.domain.Producto;
import cl.skinspad.catalogo.dto.ReservaRequest;
import cl.skinspad.catalogo.repository.CategoriaRepository;
import cl.skinspad.catalogo.repository.ProductoRepository;
import cl.skinspad.catalogo.service.CatalogoService;
import cl.skinspad.catalogo.service.TrasladoInventarioService;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:catalogo-inventario;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=20000",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "CATALOGO_TRASLADO_INICIAL=false"
})
@AutoConfigureMockMvc
class InventarioServiceTest {
  @Autowired MockMvc mvc;
  @Autowired InventarioService service;
  @Autowired ProductoRepository productos;
  @Autowired CategoriaRepository categorias;
  @Autowired CatalogoService catalogo;
  @Autowired TrasladoInventarioService traslado;
  @MockitoBean InventarioAuth auth;
  @MockitoBean InventarioClient inventario;

  private InventarioAuth.Cuenta cuenta(String id) {
    return new InventarioAuth.Cuenta(id, "cliente-" + id, "cliente", false, true);
  }

  private void permitir(String usuario, Long id) {
    when(inventario.verificarDisponible(usuario, id)).thenReturn(new InventarioClient.Item(
        id, "AK-47 | Redline (Field-Tested)", "rifle", "clasificado", "ft", false, null, true, null));
  }

  @Test
  void apiValidaPrecio() throws Exception {
    when(auth.validar("Bearer test")).thenReturn(cuenta("http"));
    mvc.perform(post("/api/inventario/publicaciones")
            .header("Authorization", "Bearer test")
            .contentType("application/json").content("{\"itemId\":1,\"precio\":0}"))
        .andExpect(status().isBadRequest());
    verifyNoInteractions(inventario);
  }

  @Test
  void publicarCreaUnaPublicacionPendienteYEvitaDuplicados() {
    permitir("demo-a", 11L);
    var p = service.publicar(cuenta("demo-a"), 11L, BigDecimal.TEN);
    assertEquals(EstadoProducto.PENDIENTE_APROBACION, p.getEstado());
    assertEquals(11L, p.getInventarioId());
    assertThrows(IllegalStateException.class,
        () -> service.publicar(cuenta("demo-a"), 11L, BigDecimal.TEN));
  }

  @Test
  void dosPublicacionesSimultaneasCreanUnaSolaPublicacion() throws Exception {
    permitir("concurrente", 12L);
    var inicio = new CountDownLatch(1);
    var pool = Executors.newFixedThreadPool(2);
    try {
      Callable<Boolean> publicar = () -> {
        inicio.await();
        try {
          service.publicar(cuenta("concurrente"), 12L, BigDecimal.TEN);
          return true;
        } catch (IllegalStateException | DataIntegrityViolationException e) {
          return false;
        }
      };
      var a = pool.submit(publicar);
      var b = pool.submit(publicar);
      inicio.countDown();
      assertNotEquals(a.get(30, TimeUnit.SECONDS), b.get(30, TimeUnit.SECONDS));
      assertEquals(1, productos.findByVendedorId("concurrente").size());
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void trasladaLasVendidasYReservadasSinPonerlasNuevamenteEnVenta() {
    var vendida = guardarPublicacionAnterior(EstadoProducto.VENDIDO, "pago-completado");
    var reservada = guardarPublicacionAnterior(EstadoProducto.INACTIVO, "pago-reservado");
    var retirada = guardarPublicacionAnterior(EstadoProducto.ELIMINADO, null);
    var idVendida = 901L;
    var idReservada = 902L;
    var idRetirada = 903L;
    when(inventario.guardarTraslado(eq("vendedor-historico"), any()))
        .thenReturn(idVendida, idReservada, idRetirada);

    var ids = List.of(vendida.getId(), reservada.getId(), retirada.getId());
    assertTrue(traslado.pendientes().containsAll(ids));
    ids.forEach(traslado::trasladar);
    ids.forEach(traslado::trasladar);
    verify(inventario, times(3)).guardarTraslado(eq("vendedor-historico"), any());
    assertEquals(List.of(
        new InventarioService.Publicacion(idVendida, vendida.getId(), "VENDIDO", false),
        new InventarioService.Publicacion(idReservada, reservada.getId(), "RESERVADO", false),
        new InventarioService.Publicacion(idRetirada, retirada.getId(), "ELIMINADO", true)),
        service.estados("vendedor-historico"));
    assertEquals("pago-completado", catalogo.buscarPorId(vendida.getId()).getReservaPago());

    var cancelar = new ReservaRequest("pago-reservado", List.of(reservada.getId()), "DESCARTAR");
    catalogo.reservar(cancelar);
    catalogo.reservar(cancelar);
    var libre = catalogo.buscarPorId(reservada.getId());
    assertNull(libre.getReservaPago());
    assertEquals(EstadoProducto.ELIMINADO, libre.getEstado());
    assertTrue(service.estados("vendedor-historico").get(1).libre());
  }

  private Producto guardarPublicacionAnterior(EstadoProducto estado, String reserva) {
    var producto = new Producto();
    producto.setNombre("AK-47 | Redline");
    producto.setPrecio(BigDecimal.TEN);
    producto.setCategoria(categorias.findByNombreIgnoreCase("Rifles").orElseThrow());
    producto.setVendedorId("vendedor-historico");
    producto.setEstado(estado);
    producto.setReservaPago(reserva);
    producto.setTrasladoPendiente(null);
    return productos.saveAndFlush(producto);
  }
}
