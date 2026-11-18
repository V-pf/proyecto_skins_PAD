package cl.skinspad.catalogo.inventario;

import cl.skinspad.catalogo.domain.*;
import cl.skinspad.catalogo.repository.*;
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
  private final VinculoRepository vinculos;
  private final ItemInventarioRepository items;
  private final ProductoRepository productos;
  private final CategoriaRepository categorias;
  private final SteamClient steam;

  @Value("${INVENTARIO_DEMO:false}")
  private boolean demoHabilitado;

  public record ItemVista(ItemInventario item, String estadoPublicacion, boolean publicable) {}

  public record Inventario(
      String steamId, boolean demo, Instant sincronizado, List<ItemVista> items) {}

  @Transactional
  public Inventario consultar(String usuarioId) {
    var v = vinculos.findById(usuarioId).orElse(null);
    if (v == null && demoHabilitado) {
      prepararDemo(usuarioId);
      v = vinculos.findById(usuarioId).orElseThrow();
    }
    if (v == null || v.isDemo() && !demoHabilitado)
      return new Inventario(null, false, null, List.of());
    var publicaciones = new HashMap<Long, Producto>();
    productos.findByVendedorId(usuarioId).forEach(p -> publicaciones.put(p.getId(), p));
    boolean modoDemo = v.isDemo();
    var listado =
        items.findByUsuarioIdOrderById(usuarioId).stream()
            .filter(i -> i.isDemo() == modoDemo)
            .map(
                i -> {
                  Producto p = publicaciones.get(i.getProductoId());
                  boolean libre =
                      p == null
                          || (p.getReservaPago() == null
                              && (p.getEstado() == EstadoProducto.ELIMINADO
                                  || p.getEstado() == EstadoProducto.RECHAZADO));
                  return new ItemVista(
                      i,
                      p == null ? null : p.getEstado().name(),
                      i.isPresente() && i.isIntercambiable() && i.getTipo() != null && libre);
                })
            .toList();
    return new Inventario(v.getSteamId(), v.isDemo(), v.getSincronizado(), listado);
  }

  @Transactional
  public void sincronizar(String usuarioId) {
    var v =
        vinculos
            .bloquear(usuarioId)
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Vincula tu cuenta de Steam para importar el inventario."));
    if (v.isDemo()) {
      if (!demoHabilitado) throw new IllegalStateException("El modo de prueba está desactivado.");
      return;
    }
    var importados = steam.inventario(v.getSteamId());
    var anteriores = items.findByUsuarioIdOrderById(usuarioId);
    var indice = new HashMap<String, ItemInventario>();
    anteriores.forEach(
        i -> {
          i.setPresente(false);
          indice.put(i.getAssetId(), i);
        });
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
      items.save(item);
      if (item.isIntercambiable() && item.getProductoId() != null) {
        var publicacion = productos.bloquear(item.getProductoId()).orElseThrow();
        if (publicacion.getReservaPago() == null
            && publicacion.getEstado() == EstadoProducto.INACTIVO
            && "El ítem dejó de estar disponible en Steam."
                .equals(publicacion.getMotivoRechazo())) {
          publicacion.setEstado(EstadoProducto.PENDIENTE_APROBACION);
          publicacion.setMotivoRechazo(null);
        }
      }
    }
    for (var anterior : anteriores) {
      if ((!anterior.isPresente() || !anterior.isIntercambiable())
          && anterior.getProductoId() != null) {
        var p = productos.bloquear(anterior.getProductoId()).orElseThrow();
        if (p.getReservaPago() == null
            && (p.getEstado() == EstadoProducto.ACTIVO
                || p.getEstado() == EstadoProducto.PENDIENTE_APROBACION)) {
          p.setEstado(EstadoProducto.INACTIVO);
          p.setMotivoRechazo("El ítem dejó de estar disponible en Steam.");
        }
      }
    }
    v.setSincronizado(Instant.now());
  }

  public void verificarDisponible(Producto producto) {
    var item = items.findById(producto.getInventarioId()).orElseThrow();
    if (!item.isPresente() || !item.isIntercambiable())
      throw new IllegalStateException("El ítem ya no está disponible en el inventario.");
    if (item.isDemo()) {
      if (!demoHabilitado) throw new IllegalStateException("El modo de prueba está desactivado.");
      return;
    }
    var vinculo = vinculos.findById(producto.getVendedorId()).orElseThrow();
    boolean disponible =
        steam.inventario(vinculo.getSteamId()).stream()
            .anyMatch(i -> i.getAssetId().equals(item.getAssetId()) && i.isIntercambiable());
    if (!disponible) throw new IllegalStateException("El ítem ya no está disponible en Steam.");
  }

  @Transactional
  public Producto publicar(InventarioAuth.Cuenta cuenta, Long itemId, BigDecimal precio) {
    if (!cuenta.verificado())
      throw new IllegalStateException(
          "Tu cuenta debe ser verificada por administración antes de publicar.");
    sincronizar(cuenta.id());
    var v = vinculos.findById(cuenta.id()).orElseThrow();
    var item =
        items.findById(itemId).orElseThrow(() -> new IllegalArgumentException("Ítem inexistente."));
    if (!cuenta.id().equals(item.getUsuarioId()) || item.isDemo() != v.isDemo())
      throw new IllegalArgumentException("Este ítem no pertenece a tu inventario.");
    if (!item.isPresente() || !item.isIntercambiable() || item.getTipo() == null)
      throw new IllegalStateException("Este ítem no está disponible para vender.");
    Producto p =
        item.getProductoId() == null
            ? new Producto()
            : productos.bloquear(item.getProductoId()).orElseThrow();
    if (p.getId() != null
        && (p.getReservaPago() != null
            || (p.getEstado() != EstadoProducto.ELIMINADO
                && p.getEstado() != EstadoProducto.RECHAZADO)))
      throw new IllegalStateException("El ítem ya está publicado, reservado o vendido.");
    String categoriaNombre =
        switch (item.getTipo()) {
          case "rifle" -> "Rifles";
          case "francotirador" -> "Rifles de francotirador";
          case "pistola" -> "Pistolas";
          case "subfusil" -> "Subfusiles";
          case "escopeta" -> "Escopetas";
          case "ametralladora" -> "Ametralladoras";
          case "cuchillo" -> "Cuchillos";
          case "guantes" -> "Guantes";
          default -> "Stickers";
        };
    var categoria =
        categorias.findAll().stream()
            .filter(c -> c.getNombre().equalsIgnoreCase(categoriaNombre) && c.isEstadoHabilitada())
            .findFirst()
            .orElseThrow(
                () -> new IllegalStateException("La categoría de este ítem no está habilitada."));
    p.setCategoria(categoria);
    p.setNombre(item.getNombre());
    p.setVendedorId(cuenta.id());
    p.setVendedor(cuenta.username());
    p.setTipo(item.getTipo());
    p.setGrado(item.getGrado());
    p.setDesgaste(item.getDesgaste());
    p.setStattrak(item.isStattrak());
    p.setPrecio(precio);
    p.setEstado(EstadoProducto.PENDIENTE_APROBACION);
    p.setMotivoRechazo(null);
    p.setInventarioId(item.getId());
    p.setDemo(item.isDemo());
    p.setDescripcion(
        item.isDemo()
            ? "Ítem de prueba del inventario de demostración."
            : "Ítem importado del inventario de Steam.");
    p.getImagenes().clear();
    if (item.getImagen() != null) {
      var imagen = new ImagenProducto();
      imagen.setProducto(p);
      imagen.setUrlImagen(item.getImagen());
      imagen.setEsPrincipal(true);
      p.getImagenes().add(imagen);
    }
    productos.saveAndFlush(p);
    item.setProductoId(p.getId());
    return p;
  }

  @Transactional
  public void prepararDemo(String usuarioId) {
    if (!demoHabilitado || vinculos.existsById(usuarioId)) return;
    var v = new VinculoSteam();
    v.setUsuarioId(usuarioId);
    v.setDemo(true);
    v.setSincronizado(Instant.now());
    vinculos.saveAndFlush(v);
    String[] nombres = {
      "AK-47 | Redline (Field-Tested)",
      "AWP | Asiimov (Field-Tested)",
      "USP-S | Kill Confirmed (Minimal Wear)",
      "StatTrak™ M4A1-S | Cyrex (Minimal Wear)",
      "Glock-18 | Fade (Factory New)"
    };
    String[] tipos = {"rifle", "francotirador", "pistola", "rifle", "pistola"};
    String[] desgastes = {"ft", "ft", "mw", "mw", "fn"};
    for (int n = 0; n < nombres.length; n++) {
      var i = new ItemInventario();
      i.setUsuarioId(usuarioId);
      i.setAssetId("demo-" + usuarioId + "-" + n);
      i.setNombre(nombres[n]);
      i.setTipo(tipos[n]);
      i.setDesgaste(desgastes[n]);
      i.setGrado(n == 0 || n == 3 ? "clasificado" : "encubierto");
      i.setStattrak(n == 3);
      i.setDemo(true);
      i.setPresente(true);
      i.setIntercambiable(n != 4);
      items.save(i);
    }
  }
}
