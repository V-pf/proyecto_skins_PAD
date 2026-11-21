package cl.skinspad.catalogo.inventario;

import cl.skinspad.catalogo.domain.*;
import cl.skinspad.catalogo.repository.*;
import java.math.BigDecimal;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InventarioService {
  private static final String NO_DISPONIBLE = "El ítem dejó de estar disponible en Steam.";
  private final ProductoRepository productos;
  private final CategoriaRepository categorias;
  private final InventarioClient inventario;

  public record Publicacion(Long itemId, Long productoId, String estado, boolean libre) {}

  private boolean permiteRepublicar(Producto producto) {
    return producto.getReservaPago() == null
        && (producto.getEstado() == EstadoProducto.ELIMINADO
            || producto.getEstado() == EstadoProducto.RECHAZADO);
  }

  @Transactional(readOnly = true)
  public List<Publicacion> estados(String usuarioId) {
    return productos.findByVendedorIdAndInventarioIdIsNotNullOrderById(usuarioId).stream()
        .map(p -> {
          String estado = p.getReservaPago() != null && p.getEstado() != EstadoProducto.VENDIDO
              ? "RESERVADO" : p.getEstado().name();
          return new Publicacion(p.getInventarioId(), p.getId(), estado, permiteRepublicar(p));
        })
        .toList();
  }

  @Transactional
  public void actualizarDisponibilidad(String usuarioId, Set<Long> disponibles) {
    for (Producto p : productos.bloquearPublicaciones(usuarioId)) {
      if (p.getReservaPago() != null) continue;
      if (!disponibles.contains(p.getInventarioId())
          && (p.getEstado() == EstadoProducto.ACTIVO
              || p.getEstado() == EstadoProducto.PENDIENTE_APROBACION)) {
        p.setEstado(EstadoProducto.INACTIVO);
        p.setMotivoRechazo(NO_DISPONIBLE);
      } else if (disponibles.contains(p.getInventarioId())
          && p.getEstado() == EstadoProducto.INACTIVO
          && NO_DISPONIBLE.equals(p.getMotivoRechazo())) {
        p.setEstado(EstadoProducto.PENDIENTE_APROBACION);
        p.setMotivoRechazo(null);
      }
    }
  }

  public void verificarDisponible(Producto producto) {
    inventario.verificarDisponible(producto.getVendedorId(), producto.getInventarioId());
  }

  @Transactional
  public Producto publicar(InventarioAuth.Cuenta cuenta, Long itemId, BigDecimal precio) {
    if (!cuenta.verificado()) {
      throw new IllegalStateException(
          "Tu cuenta debe ser verificada por administración antes de publicar.");
    }
    var item = inventario.verificarDisponible(cuenta.id(), itemId);
    Producto p = item.productoOrigenId() == null
        ? productos.bloquearPorInventario(itemId).orElseGet(Producto::new)
        : productos.bloquear(item.productoOrigenId())
            .filter(origen -> itemId.equals(origen.getInventarioId()))
            .orElseThrow(() -> new IllegalStateException("El traslado de este ítem sigue en curso."));
    if (p.getId() != null
        && (!cuenta.id().equals(p.getVendedorId()) || !permiteRepublicar(p))) {
      throw new IllegalStateException("El ítem ya está publicado, reservado o vendido.");
    }
    Categoria categoria = item.productoOrigenId() == null
        ? categoriaPara(item.tipo()) : p.getCategoria();
    if (!categoria.isEstadoHabilitada()) {
      throw new IllegalStateException("La categoría de este ítem no está habilitada.");
    }
    p.setCategoria(categoria);
    p.setNombre(item.nombre());
    p.setVendedorId(cuenta.id());
    p.setVendedor(cuenta.username());
    p.setTipo(item.tipo());
    p.setGrado(item.grado());
    p.setDesgaste(item.desgaste());
    p.setStattrak(item.stattrak());
    p.setPrecio(precio);
    p.setEstado(EstadoProducto.PENDIENTE_APROBACION);
    p.setRetiradaParaInventario(false);
    p.setMotivoRechazo(null);
    p.setInventarioId(item.id());
    p.setDemo(item.demo());
    p.setDescripcion(
        item.demo()
            ? "Ítem de prueba del inventario de demostración."
            : "Ítem importado del inventario de Steam.");
    if (item.productoOrigenId() == null) {
      p.getImagenes().clear();
      if (item.imagen() != null) {
        var imagen = new ImagenProducto();
        imagen.setProducto(p);
        imagen.setUrlImagen(item.imagen());
        imagen.setEsPrincipal(true);
        p.getImagenes().add(imagen);
      }
    }
    productos.saveAndFlush(p);
    return p;
  }

  private Categoria categoriaPara(String tipo) {
    String categoriaNombre =
        switch (tipo) {
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
    return categorias.findByNombreIgnoreCase(categoriaNombre)
        .orElseThrow(() -> new IllegalStateException("La categoría de este ítem no está habilitada."));
  }
}
