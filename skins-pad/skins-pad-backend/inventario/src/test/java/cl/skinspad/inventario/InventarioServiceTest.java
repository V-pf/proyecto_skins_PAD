package cl.skinspad.inventario;

import cl.skinspad.inventario.client.ServiciosInternos;
import cl.skinspad.inventario.domain.ItemInventario;
import cl.skinspad.inventario.repository.ItemInventarioRepository;
import cl.skinspad.inventario.service.InventarioService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:inventario;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class InventarioServiceTest {
  @Autowired InventarioService service;
  @Autowired ItemInventarioRepository items;
  @MockitoBean ServiciosInternos servicios;

  @Test
  void trasladoEsIdempotenteYElItemSoloApareceEnElInventarioDeSuDueno() {
    var item = service.guardarTraslado("dueno", 42L, null, "AK-47 | Redline", "rifle",
        "clasificado", "ft", false, null, null);
    assertEquals("catalogo-42", item.getAssetId());
    assertEquals(42L, item.getProductoOrigenId());
    assertEquals(item.getId(), service.guardarTraslado("dueno", 42L, null, "AK-47 | Redline",
        "rifle", "clasificado", "ft", false, null, null).getId());
    assertTrue(service.consultar("dueno").items().isEmpty());
    when(servicios.estados("dueno")).thenReturn(List.of(
        new ServiciosInternos.Publicacion(item.getId(), 42L, "ELIMINADO", true)));
    var vista = service.consultar("dueno");
    assertEquals(1, vista.items().size());
    assertTrue(vista.items().get(0).publicable());
    assertTrue(service.consultar("ajeno").items().isEmpty());
    assertThrows(IllegalArgumentException.class,
        () -> service.verificarDisponible("ajeno", item.getId()));
    assertEquals(item.getId(), service.verificarDisponible("dueno", item.getId()).getId());
  }

  @Test
  void reintentarElTrasladoReutilizaElInventarioPrecargado() {
    var precargado = new ItemInventario();
    precargado.setUsuarioId("dueno-precarga");
    precargado.setAssetId("precarga-1");
    precargado.setNombre("AK-47 | Redline");
    Long id = items.saveAndFlush(precargado).getId();

    var trasladado = service.guardarTraslado("dueno-precarga", 77L, id, "AK-47 | Redline",
        "rifle", "clasificado", "ft", false, null, null);
    assertEquals(id, trasladado.getId());
    assertEquals("catalogo-77-" + id, trasladado.getAssetId());
    assertEquals(77L, trasladado.getProductoOrigenId());

    var reintento = service.guardarTraslado("dueno-precarga", 77L, null, "AK-47 | Redline",
        "rifle", "clasificado", "ft", false, null, null);
    assertEquals(id, reintento.getId());
    assertEquals(1, items.findByUsuarioIdOrderById("dueno-precarga").size());
  }

  @Test
  void unaVentaOcultaElItemDelInventarioYConservaSuRegistro() {
    var vendida = service.guardarTraslado("otro-dueno", 43L, null, "AK-47", "rifle",
        "clasificado", "ft", false, null, null);
    var reservada = service.guardarTraslado("otro-dueno", 44L, null, "AWP", "francotirador",
        "clasificado", "ft", false, null, null);
    when(servicios.estados("otro-dueno")).thenReturn(List.of(
        new ServiciosInternos.Publicacion(vendida.getId(), 43L, "VENDIDO", false),
        new ServiciosInternos.Publicacion(reservada.getId(), 44L, "RESERVADO", false)));

    var vista = service.consultar("otro-dueno");
    assertEquals(List.of(reservada.getId()), vista.items().stream().map(i -> i.item().getId()).toList());
    assertTrue(items.existsById(vendida.getId()));
  }

  @Test
  void conservaYValidaLocalmenteLosItemsImportadosConAnterioridad() {
    var item = new ItemInventario();
    item.setUsuarioId("dueno-legado");
    item.setAssetId("100");
    item.setNombre("AK-47 | Redline (Field-Tested)");
    item.setTipo("rifle");
    item.setGrado("clasificado");
    item.setDesgaste("ft");
    item.setIntercambiable(true);
    item.setPresente(true);
    Long id = items.saveAndFlush(item).getId();
    assertEquals(id, service.verificarDisponible("dueno-legado", id).getId());
    verifyNoInteractions(servicios);
    assertTrue(service.consultar("dueno-legado").items().get(0).publicable());

    item.setPresente(false);
    items.saveAndFlush(item);
    assertFalse(service.consultar("dueno-legado").items().get(0).publicable());
    assertThrows(IllegalStateException.class, () -> service.verificarDisponible("dueno-legado", id));
  }
}
