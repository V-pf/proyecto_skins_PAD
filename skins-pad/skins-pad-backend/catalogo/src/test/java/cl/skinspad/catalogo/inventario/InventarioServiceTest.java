package cl.skinspad.catalogo.inventario;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import cl.skinspad.catalogo.domain.EstadoProducto;
import cl.skinspad.catalogo.repository.ProductoRepository;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:inventario;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=20000",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "INVENTARIO_DEMO=true"
    })
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
class InventarioServiceTest {
  @Autowired org.springframework.test.web.servlet.MockMvc mvc;
  @MockitoBean InventarioAuth auth;
  @Autowired InventarioService service;
  @Autowired VinculoRepository vinculos;
  @Autowired ItemInventarioRepository items;
  @Autowired ProductoRepository productos;
  @MockitoBean SteamClient steam;

  @Test
  void apiValidaPrecioYRechazaUnaVinculacionFalsa() throws Exception {
    when(auth.validar("Bearer test")).thenReturn(cuenta("http"));
    Long item = service.consultar("http").items().get(0).item().getId();
    mvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                    "/api/inventario")
                .header("Authorization", "Bearer test"))
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath(
                    "$.items.length()")
                .value(5));
    mvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                    "/api/inventario/publicaciones")
                .header("Authorization", "Bearer test")
                .contentType("application/json")
                .content("{\"itemId\":" + item + ",\"precio\":0}"))
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
                .isBadRequest());
    mvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                    "/api/steam/callback")
                .param("state", "inventado")
                .param(
                    "openid.claimed_id", "https://steamcommunity.com/openid/id/76561198000000001"))
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isSeeOther())
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                .string("Location", "http://localhost:5173/cliente/inventario?steam=error"));
  }

  private InventarioAuth.Cuenta cuenta(String id) {
    return new InventarioAuth.Cuenta(id, "cliente-" + id, "cliente", false, true);
  }

  @Test
  void creaInventarioIndividualYEvitaPublicarItemsAjenosOBloqueados() {
    var a = service.consultar("demo-a");
    var b = service.consultar("demo-b");
    assertEquals(5, a.items().size());
    assertEquals(5, b.items().size());
    assertThrows(
        IllegalArgumentException.class,
        () -> service.publicar(cuenta("demo-b"), a.items().get(0).item().getId(), BigDecimal.TEN));
    assertThrows(
        IllegalStateException.class,
        () -> service.publicar(cuenta("demo-a"), a.items().get(4).item().getId(), BigDecimal.TEN));
    var p = service.publicar(cuenta("demo-a"), a.items().get(0).item().getId(), BigDecimal.TEN);
    assertEquals(EstadoProducto.PENDIENTE_APROBACION, p.getEstado());
    assertEquals(a.items().get(0).item().getNombre(), p.getNombre());
    assertThrows(
        IllegalStateException.class,
        () -> service.publicar(cuenta("demo-a"), a.items().get(0).item().getId(), BigDecimal.TEN));
    assertEquals(5, service.consultar("demo-a").items().size());
  }

  @Test
  void dosPublicacionesSimultaneasCreanUnaSolaPublicacion() throws Exception {
    Long item = service.consultar("concurrente").items().get(0).item().getId();
    var inicio = new CountDownLatch(1);
    var pool = Executors.newFixedThreadPool(2);
    try {
      Callable<Boolean> publicar =
          () -> {
            inicio.await();
            try {
              service.publicar(cuenta("concurrente"), item, BigDecimal.TEN);
              return true;
            } catch (IllegalStateException e) {
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
  void falloDeSteamConservaInventarioYUnaSincronizacionCompletaRetiraAusentes() {
    var v = new VinculoSteam();
    v.setUsuarioId("real");
    v.setSteamId("76561198000000001");
    vinculos.save(v);
    var item = new ItemInventario();
    item.setAssetId("100");
    item.setNombre("AK-47 | Redline (Field-Tested)");
    item.setTipo("rifle");
    item.setGrado("clasificado");
    item.setDesgaste("ft");
    item.setPresente(true);
    item.setIntercambiable(true);
    when(steam.inventario(v.getSteamId())).thenReturn(List.of(item));
    service.sincronizar("real");
    Long id = service.consultar("real").items().get(0).item().getId();
    when(steam.inventario(v.getSteamId()))
        .thenThrow(new IllegalStateException("Steam no disponible"));
    assertThrows(IllegalStateException.class, () -> service.sincronizar("real"));
    assertTrue(items.findById(id).orElseThrow().isPresente());
    doReturn(List.of()).when(steam).inventario(v.getSteamId());
    service.sincronizar("real");
    assertFalse(items.findById(id).orElseThrow().isPresente());
  }
}
