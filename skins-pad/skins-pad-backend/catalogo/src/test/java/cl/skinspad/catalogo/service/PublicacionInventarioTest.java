package cl.skinspad.catalogo.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.skinspad.catalogo.client.InventarioAuth;
import cl.skinspad.catalogo.client.InventarioClient;
import cl.skinspad.catalogo.domain.EstadoProducto;
import cl.skinspad.catalogo.domain.ImagenProducto;
import cl.skinspad.catalogo.dto.EstadoPublicacionInventario;
import cl.skinspad.catalogo.domain.Producto;
import cl.skinspad.catalogo.dto.ReservaRequest;
import cl.skinspad.catalogo.repository.CategoriaRepository;
import cl.skinspad.catalogo.repository.ProductoRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:catalogo-inventario;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=20000",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "CATALOGO_TRASLADO_INICIAL=false"
})
@AutoConfigureMockMvc
class PublicacionInventarioTest {
  @Autowired MockMvc mvc;
  @Autowired ProductoRepository productos;
  @Autowired CategoriaRepository categorias;
  @Autowired CatalogoService catalogo;
  @Autowired org.springframework.transaction.support.TransactionTemplate transaccion;
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
  void publicarCreaUnaPublicacionActivaYEvitaDuplicados() {
    permitir("demo-a", 11L);
    var p = catalogo.publicarDesdeInventario(cuenta("demo-a"), 11L, BigDecimal.TEN);
    assertEquals(EstadoProducto.ACTIVO, p.getEstado());
    assertNull(p.getRevisionApta());
    assertEquals(11L, p.getInventarioId());
    assertThrows(IllegalStateException.class,
        () -> catalogo.publicarDesdeInventario(cuenta("demo-a"), 11L, BigDecimal.TEN));

    catalogo.actualizarDisponibilidadInventario("demo-a", Set.of());
    assertEquals(EstadoProducto.INACTIVO, catalogo.buscarPorId(p.getId()).getEstado());
    assertThrows(IllegalStateException.class,
        () -> catalogo.publicarDesdeInventario(cuenta("demo-a"), 11L, BigDecimal.TEN));
    catalogo.actualizarDisponibilidadInventario("demo-a", Set.of(11L));
    assertEquals(EstadoProducto.ACTIVO, catalogo.buscarPorId(p.getId()).getEstado());
    assertEquals(1, productos.findByVendedorId("demo-a").size());
  }

  @Test
  void moderarImpideRepublicarHastaQueSeMarqueApta() {
    var vendedor = cuenta("moderado");
    permitir(vendedor.id(), 15L);
    var p = catalogo.publicarDesdeInventario(vendedor, 15L, BigDecimal.TEN);
    catalogo.actualizarEstado(p.getId(), EstadoProducto.RECHAZADO, "Motivo de prueba");
    assertFalse(catalogo.estadosDeInventario(vendedor.id()).get(0).libre());
    assertThrows(IllegalStateException.class,
        () -> catalogo.publicarDesdeInventario(vendedor, 15L, BigDecimal.TEN));

    var revisada = catalogo.actualizarEstado(p.getId(), EstadoProducto.ACTIVO);
    assertEquals(EstadoProducto.RECHAZADO, revisada.getEstado());
    assertTrue(revisada.getRevisionApta());
    var nueva = catalogo.publicarDesdeInventario(vendedor, 15L, BigDecimal.TEN);
    assertEquals(EstadoProducto.ACTIVO, nueva.getEstado());
    assertNotEquals(p.getId(), nueva.getId());
    assertEquals(EstadoProducto.RECHAZADO, catalogo.buscarPorId(p.getId()).getEstado());
  }

  @Test
  void venderUnaSkinTrasladadaMantieneElHistorialDeSuAnuncioAnterior() {
    var vendedor = cuenta("trasladado");
    Long itemId = 18L;
    var anterior = guardarPublicacionAnterior(EstadoProducto.ELIMINADO, null);
    anterior.setVendedorId(vendedor.id());
    anterior.setInventarioId(itemId);
    anterior.setTrasladoPendiente(false);
    anterior.setFloatValor(new BigDecimal("0.28740"));
    var portada = new ImagenProducto();
    portada.setProducto(anterior);
    portada.setUrlImagen("https://ejemplo.test/portada.png");
    portada.setEsPrincipal(true);
    anterior.getImagenes().add(portada);
    anterior = productos.saveAndFlush(anterior);
    Long anteriorId = anterior.getId();
    when(inventario.verificarDisponible(vendedor.id(), itemId)).thenReturn(new InventarioClient.Item(
        itemId, anterior.getNombre(), "rifle", "clasificado", "ft", false, null, true, anteriorId));

    var nueva = catalogo.publicarDesdeInventario(vendedor, itemId, BigDecimal.valueOf(2000));
    assertNotEquals(anteriorId, nueva.getId());
    assertEquals(itemId, nueva.getInventarioId());
    assertEquals(anterior.getFloatValor(), nueva.getFloatValor());
    assertEquals(portada.getUrlImagen(), nueva.getImagenes().get(0).getUrlImagen());
    assertNotEquals(portada.getId(), nueva.getImagenes().get(0).getId());
    assertEquals(List.of(new EstadoPublicacionInventario(itemId, nueva.getId(), "ACTIVO", false)),
        catalogo.estadosDeInventario(vendedor.id()));

    String pago = "pago-trasladado";
    var ids = List.of(nueva.getId());
    catalogo.reservar(new ReservaRequest(pago, ids, "RESERVAR"));
    assertEquals(List.of(new EstadoPublicacionInventario(itemId, nueva.getId(), "RESERVADO", false)),
        catalogo.estadosDeInventario(vendedor.id()));
    assertThrows(IllegalStateException.class,
        () -> catalogo.publicarDesdeInventario(vendedor, itemId, BigDecimal.TEN));
    catalogo.reservar(new ReservaRequest(pago, ids, "DESCARTAR"));
    assertEquals(EstadoProducto.ACTIVO, catalogo.buscarPorId(nueva.getId()).getEstado());
    catalogo.reservar(new ReservaRequest(pago, ids, "RESERVAR"));
    catalogo.reservar(new ReservaRequest(pago, ids, "VENDER"));
    assertEquals(List.of(new EstadoPublicacionInventario(itemId, nueva.getId(), "VENDIDO", false)),
        catalogo.estadosDeInventario(vendedor.id()));
    assertThrows(IllegalStateException.class,
        () -> catalogo.publicarDesdeInventario(vendedor, itemId, BigDecimal.TEN));

    var guardada = catalogo.buscarPorId(anteriorId);
    assertEquals(EstadoProducto.ELIMINADO, guardada.getEstado());
    assertEquals(0, anterior.getPrecio().compareTo(guardada.getPrecio()));
    assertEquals(pago, catalogo.buscarPorId(nueva.getId()).getReservaPago());
  }

  @Test
  void activaPendientesVinculadasSinPublicarRegistrosHistoricos() {
    var vendedor = cuenta("pendiente");
    permitir(vendedor.id(), 17L);
    var vinculada = catalogo.publicarDesdeInventario(vendedor, 17L, BigDecimal.TEN);
    vinculada.setEstado(EstadoProducto.PENDIENTE_APROBACION);
    productos.saveAndFlush(vinculada);
    var historica = guardarPublicacionAnterior(EstadoProducto.PENDIENTE_APROBACION, null);

    transaccion.executeWithoutResult(status -> productos.activarPendientesDeInventario(
        EstadoProducto.PENDIENTE_APROBACION, EstadoProducto.ACTIVO));

    assertEquals(EstadoProducto.ACTIVO, catalogo.buscarPorId(vinculada.getId()).getEstado());
    assertEquals(EstadoProducto.PENDIENTE_APROBACION, catalogo.buscarPorId(historica.getId()).getEstado());
  }

  @Test
  void dosRepublicacionesSimultaneasCreanUnSoloAnuncioNuevo() throws Exception {
    permitir("concurrente", 12L);
    var retirada = catalogo.publicarDesdeInventario(cuenta("concurrente"), 12L, BigDecimal.TEN);
    catalogo.eliminarProducto(retirada.getId(), "Retiro del vendedor", false);
    var inicio = new CountDownLatch(1);
    var pool = Executors.newFixedThreadPool(2);
    try {
      Callable<Boolean> publicar = () -> {
        inicio.await();
        try {
          catalogo.publicarDesdeInventario(cuenta("concurrente"), 12L, BigDecimal.TEN);
          return true;
        } catch (IllegalStateException e) {
          return false;
        }
      };
      var a = pool.submit(publicar);
      var b = pool.submit(publicar);
      inicio.countDown();
      assertNotEquals(a.get(30, TimeUnit.SECONDS), b.get(30, TimeUnit.SECONDS));
      assertEquals(2, productos.findByVendedorId("concurrente").size());
      assertEquals(EstadoProducto.ELIMINADO, catalogo.buscarPorId(retirada.getId()).getEstado());
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
        new EstadoPublicacionInventario(idVendida, vendida.getId(), "VENDIDO", false),
        new EstadoPublicacionInventario(idReservada, reservada.getId(), "RESERVADO", false),
        new EstadoPublicacionInventario(idRetirada, retirada.getId(), "ELIMINADO", true)),
        catalogo.estadosDeInventario("vendedor-historico"));
    assertEquals("pago-completado", catalogo.buscarPorId(vendida.getId()).getReservaPago());

    var cancelar = new ReservaRequest("pago-reservado", List.of(reservada.getId()), "DESCARTAR");
    catalogo.reservar(cancelar);
    catalogo.reservar(cancelar);
    var libre = catalogo.buscarPorId(reservada.getId());
    assertNull(libre.getReservaPago());
    assertEquals(EstadoProducto.ELIMINADO, libre.getEstado());
    assertTrue(catalogo.estadosDeInventario("vendedor-historico").get(1).libre());
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
