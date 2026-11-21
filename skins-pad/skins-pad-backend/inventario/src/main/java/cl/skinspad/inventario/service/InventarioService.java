package cl.skinspad.inventario.service;

import cl.skinspad.inventario.client.ServiciosInternos;
import cl.skinspad.inventario.domain.ItemInventario;
import cl.skinspad.inventario.domain.SincronizacionInventario;
import cl.skinspad.inventario.repository.ItemInventarioRepository;
import cl.skinspad.inventario.repository.SincronizacionRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InventarioService {
  private final SincronizacionRepository sincronizaciones;
  private final ItemInventarioRepository items;
  private final ServiciosInternos servicios;

  @Value("${INVENTARIO_DEMO:false}")
  private boolean demoHabilitado;

  public record ItemVista(ItemInventario item, String estadoPublicacion, boolean publicable) {}
  public record Inventario(
      String steamId, boolean demo, Instant sincronizado, List<ItemVista> items) {}

  @Transactional
  public Inventario consultar(String usuarioId) {
    String steamId = servicios.steamId(usuarioId);
    var visibles = items.findByUsuarioIdOrderById(usuarioId).stream()
        .filter(item -> visible(item, steamId)).toList();
    var estados = new HashMap<Long, ServiciosInternos.Publicacion>();
    if (!visibles.isEmpty()) servicios.estados(usuarioId).forEach(p -> estados.put(p.itemId(), p));
    var listado = visibles.stream()
        .filter(i -> i.getProductoOrigenId() == null || estados.containsKey(i.getId()))
        .map(i -> {
          var p = estados.get(i.getId());
          i.setProductoId(p == null ? null : p.productoId());
          return new ItemVista(i, p == null ? null : p.estado(),
              disponible(i) && (p == null || p.libre()));
        }).toList();
    var fecha = sincronizaciones.findById(usuarioId)
        .map(SincronizacionInventario::getSincronizado).orElse(null);
    boolean demo = listado.stream().anyMatch(vista -> vista.item().isDemo());
    return new Inventario(steamId, demo, fecha, listado);
  }

  @Transactional
  public void sincronizar(String usuarioId) {
    if (servicios.steamId(usuarioId) == null) {
      throw new IllegalStateException("Vincula tu cuenta de Steam para importar el inventario.");
    }
    var sincronizacion = bloquearOCrear(usuarioId);
    var importados = servicios.inventarioSteam(usuarioId);
    var indice = new HashMap<String, ItemInventario>();
    var anteriores = items.findByUsuarioIdOrderById(usuarioId);
    anteriores.stream().filter(i -> !i.isDemo()).forEach(i -> {
      i.setPresente(false);
      indice.put(i.getAssetId(), i);
    });
    var disponibles = new HashSet<Long>();
    anteriores.stream().filter(i -> i.getProductoOrigenId() != null && disponible(i))
        .map(ItemInventario::getId).forEach(disponibles::add);
    for (var nuevo : importados) {
      var item = indice.getOrDefault(nuevo.getAssetId(), nuevo);
      item.setUsuarioId(usuarioId);
      item.setNombre(nuevo.getNombre());
      item.setTipo(nuevo.getTipo());
      item.setGrado(nuevo.getGrado());
      item.setDesgaste(nuevo.getDesgaste());
      item.setStattrak(nuevo.isStattrak());
      item.setImagen(nuevo.getImagen());
      item.setIntercambiable(nuevo.isIntercambiable());
      item.setPresente(true);
      item.setDemo(false);
      item = items.save(item);
      if (disponible(item)) disponibles.add(item.getId());
    }
    servicios.actualizarDisponibilidad(usuarioId, disponibles);
    sincronizacion.setSincronizado(Instant.now());
  }

  public ItemInventario itemDe(String usuarioId, Long id) {
    var item = items.findById(id)
        .filter(i -> usuarioId.equals(i.getUsuarioId()))
        .orElseThrow(() -> new IllegalArgumentException("El ítem no pertenece a tu inventario."));
    if (item.getProductoOrigenId() == null && !visible(item, servicios.steamId(usuarioId))) {
      throw new IllegalStateException("El ítem ya no pertenece al inventario activo.");
    }
    return item;
  }

  public ItemInventario verificarDisponible(String usuarioId, Long id) {
    var item = itemDe(usuarioId, id);
    if (!disponible(item)) {
      throw new IllegalStateException("El ítem no está disponible para vender. Sincroniza tu inventario.");
    }
    if (!item.isDemo() && servicios.inventarioSteam(usuarioId).stream().noneMatch(i ->
        i.getAssetId().equals(item.getAssetId()) && i.isIntercambiable() && i.getTipo() != null)) {
      throw new IllegalStateException("El ítem ya no está disponible en Steam.");
    }
    return item;
  }

  private boolean visible(ItemInventario item, String steamId) {
    if (item.getProductoOrigenId() != null) return true;
    return item.isDemo() ? demoHabilitado && steamId == null : steamId != null;
  }

  private boolean disponible(ItemInventario item) {
    return item.isPresente() && item.isIntercambiable()
        && (item.getTipo() != null || item.getProductoOrigenId() != null);
  }

  private SincronizacionInventario bloquearOCrear(String usuarioId) {
    return sincronizaciones.bloquear(usuarioId).orElseGet(() -> {
      var registro = new SincronizacionInventario();
      registro.setUsuarioId(usuarioId);
      return sincronizaciones.saveAndFlush(registro);
    });
  }

  @Transactional
  public ItemInventario guardarTraslado(String usuarioId, Long productoId, Long inventarioId,
      String nombre, String tipo, String grado, String desgaste, boolean stattrak,
      BigDecimal floatValor, String imagen) {
    if (inventarioId != null) {
      var existente = items.findById(inventarioId)
          .filter(item -> usuarioId.equals(item.getUsuarioId()))
          .filter(item -> item.getProductoOrigenId() == null || productoId.equals(item.getProductoOrigenId()));
      if (existente.isPresent()) {
        var item = existente.get();
        if (item.isDemo()) item.setProductoOrigenId(productoId);
        return item;
      }
    }

    String assetId = "catalogo-" + productoId;
    var item = items.findByUsuarioIdAndAssetId(usuarioId, assetId).orElseGet(ItemInventario::new);
    item.setUsuarioId(usuarioId);
    item.setAssetId(assetId);
    item.setProductoOrigenId(productoId);
    item.setNombre(nombre);
    item.setTipo(tipo);
    item.setGrado(grado);
    item.setDesgaste(desgaste);
    item.setStattrak(stattrak);
    item.setFloatValor(floatValor);
    item.setImagen(imagen);
    item.setDemo(true);
    item.setPresente(true);
    item.setIntercambiable(true);
    return items.save(item);
  }
}
