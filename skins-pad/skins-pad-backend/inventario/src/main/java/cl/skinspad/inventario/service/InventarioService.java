package cl.skinspad.inventario.service;

import cl.skinspad.inventario.client.ServiciosInternos;
import cl.skinspad.inventario.domain.ItemInventario;
import cl.skinspad.inventario.repository.ItemInventarioRepository;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InventarioService {
  private final ItemInventarioRepository items;
  private final ServiciosInternos servicios;

  public record ItemVista(ItemInventario item, String estadoPublicacion, boolean publicable) {}
  public record Inventario(List<ItemVista> items) {}

  @Transactional(readOnly = true)
  public Inventario consultar(String usuarioId) {
    var visibles = items.findByUsuarioIdOrderById(usuarioId);
    var estados = new HashMap<Long, ServiciosInternos.Publicacion>();
    if (!visibles.isEmpty()) servicios.estados(usuarioId).forEach(p -> estados.put(p.itemId(), p));
    var listado = visibles.stream()
        .filter(i -> {
          var publicacion = estados.get(i.getId());
          return publicacion == null ? i.getProductoOrigenId() == null : !"VENDIDO".equals(publicacion.estado());
        })
        .map(i -> {
          var p = estados.get(i.getId());
          i.setProductoId(p == null ? null : p.productoId());
          return new ItemVista(i, p == null ? null : p.estado(),
              disponible(i) && (p == null || p.libre()));
        }).toList();
    return new Inventario(listado);
  }

  public ItemInventario itemDe(String usuarioId, Long id) {
    return items.findById(id)
        .filter(i -> usuarioId.equals(i.getUsuarioId()))
        .orElseThrow(() -> new IllegalArgumentException("El ítem no pertenece a tu inventario."));
  }

  public ItemInventario verificarDisponible(String usuarioId, Long id) {
    var item = itemDe(usuarioId, id);
    if (!disponible(item)) {
      throw new IllegalStateException("El ítem no está disponible para vender.");
    }
    return item;
  }

  private boolean disponible(ItemInventario item) {
    return item.isPresente() && item.isIntercambiable()
        && (item.getTipo() != null || item.getProductoOrigenId() != null);
  }

  @Transactional
  public ItemInventario guardarTraslado(String usuarioId, Long productoId, Long inventarioId,
      String nombre, String tipo, String grado, String desgaste, boolean stattrak,
      BigDecimal floatValor, String imagen) {
    String assetId = ItemInventario.assetDeCatalogo(productoId);
    if (inventarioId != null) {
      var existente = items.findById(inventarioId)
          .filter(item -> usuarioId.equals(item.getUsuarioId()))
          .filter(item -> item.getProductoOrigenId() == null || productoId.equals(item.getProductoOrigenId()));
      if (existente.isPresent()) {
        var item = existente.get();
        if (item.esPrecarga()) {
          item.setAssetId(assetId + "-" + item.getId());
        }
        return item;
      }
    }

    var item = items.findByUsuarioIdAndAssetId(usuarioId, assetId)
        .or(() -> items.findFirstByUsuarioIdAndAssetIdStartingWithOrderById(usuarioId, assetId + "-"))
        .orElseGet(() -> {
          var nuevo = new ItemInventario();
          nuevo.setAssetId(assetId);
          return nuevo;
        });
    item.setUsuarioId(usuarioId);
    item.setNombre(nombre);
    item.setTipo(tipo);
    item.setGrado(grado);
    item.setDesgaste(desgaste);
    item.setStattrak(stattrak);
    item.setFloatValor(floatValor);
    item.setImagen(imagen);
    item.setPresente(true);
    item.setIntercambiable(true);
    return items.save(item);
  }
}
