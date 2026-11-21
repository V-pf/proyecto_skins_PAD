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
    assertEquals(item.getId(), service.guardarTraslado("dueno", 42L, null, "AK-47 | Redline",
        "rifle", "clasificado", "ft", false, null, null).getId());
    assertTrue(service.consultar("dueno").items().isEmpty());
    when(servicios.estados("dueno")).thenReturn(List.of(
        new ServiciosInternos.Publicacion(item.getId(), 42L, "ELIMINADO", true)));
    var vista = service.consultar("dueno");
    assertTrue(vista.demo());
    assertEquals(1, vista.items().size());
    assertTrue(vista.items().get(0).publicable());
    assertTrue(service.consultar("ajeno").items().isEmpty());
    assertThrows(IllegalArgumentException.class,
        () -> service.verificarDisponible("ajeno", item.getId()));
    when(servicios.steamId("dueno")).thenReturn("76561198000000001");
    service.sincronizar("dueno");
    assertEquals(item.getId(), service.consultar("dueno").items().get(0).item().getId());
    verify(servicios).actualizarDisponibilidad("dueno", java.util.Set.of(item.getId()));
  }

  @Test
  void falloDeSteamConservaInventarioYUnaSincronizacionCompletaRetiraAusentes() {
    when(servicios.steamId("real")).thenReturn("76561198000000001");
    var item = new ItemInventario();
    item.setAssetId("100");
    item.setNombre("AK-47 | Redline (Field-Tested)");
    item.setTipo("rifle");
    item.setGrado("clasificado");
    item.setDesgaste("ft");
    item.setIntercambiable(true);
    when(servicios.inventarioSteam("real")).thenReturn(List.of(item));
    service.sincronizar("real");
    Long id = service.consultar("real").items().get(0).item().getId();
    when(servicios.inventarioSteam("real")).thenThrow(new IllegalStateException("Steam no disponible"));
    assertThrows(IllegalStateException.class, () -> service.sincronizar("real"));
    assertTrue(items.findById(id).orElseThrow().isPresente());
    doReturn(List.of()).when(servicios).inventarioSteam("real");
    service.sincronizar("real");
    assertFalse(items.findById(id).orElseThrow().isPresente());
  }
}
