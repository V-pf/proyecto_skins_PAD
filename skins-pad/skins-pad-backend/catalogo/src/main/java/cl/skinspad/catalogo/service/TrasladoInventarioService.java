package cl.skinspad.catalogo.service;

import cl.skinspad.catalogo.domain.EstadoProducto;
import cl.skinspad.catalogo.domain.ImagenProducto;
import cl.skinspad.catalogo.client.InventarioClient;
import cl.skinspad.catalogo.repository.ProductoRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TrasladoInventarioService {
  private final ProductoRepository productos;
  private final InventarioClient inventario;

  @Transactional(readOnly = true)
  public List<Long> pendientes() {
    return productos.buscarPendientesDeTraslado();
  }

  @Transactional
  public void trasladar(Long id) {
    var producto = productos.bloquear(id).orElse(null);
    if (producto == null || producto.getInventarioId() != null
        || producto.getVendedorId() == null || producto.getVendedorId().isBlank()) {
      return;
    }

    var imagenes = producto.getImagenes();
    String portada = imagenes.stream().filter(ImagenProducto::isEsPrincipal).findFirst()
        .or(() -> imagenes.stream().findFirst()).map(ImagenProducto::getUrlImagen).orElse(null);
    Long itemId = inventario.guardarTraslado(producto.getVendedorId(),
        new InventarioClient.Traslado(id, producto.getInventarioId(), producto.getNombre(),
            producto.getTipo(), producto.getGrado(), producto.getDesgaste(),
            Boolean.TRUE.equals(producto.getStattrak()), producto.getFloatValor(), portada));
    producto.setInventarioId(itemId);
    if (producto.getEstado() != EstadoProducto.VENDIDO
        && producto.getEstado() != EstadoProducto.ELIMINADO) {
      producto.setEstado(EstadoProducto.ELIMINADO);
      if (producto.getMotivoRechazo() == null) {
        producto.setMotivoRechazo("Trasladada al inventario del vendedor.");
      }
    }
  }
}
