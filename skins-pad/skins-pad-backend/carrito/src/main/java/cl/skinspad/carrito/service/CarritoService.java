package cl.skinspad.carrito.service;

import cl.skinspad.carrito.client.CatalogoClient;
import cl.skinspad.carrito.client.ProductoRemoto;
import cl.skinspad.carrito.domain.Carrito;
import cl.skinspad.carrito.domain.ItemCarrito;
import cl.skinspad.carrito.repository.CarritoRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CarritoService {

    private final CarritoRepository carritos;
    private final CatalogoClient catalogo;

    @Transactional
    public Carrito crearCarrito(String usuarioId) {
        if (usuarioId == null || usuarioId.isBlank()) {
            throw new IllegalArgumentException("usuarioId es obligatorio");
        }

        Carrito carrito = new Carrito();
        carrito.setUsuarioId(usuarioId);
        carrito.setFechaCreacion(LocalDateTime.now());
        return carritos.save(carrito);
    }

    @Transactional(readOnly = true)
    public Carrito obtenerPorId(Long id) {
        return carritos.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Carrito no encontrado: " + id));
    }

    // Se usa el carrito más reciente o se crea uno si la cuenta aún no tiene.
    public synchronized Carrito obtenerPorUsuario(String usuarioId) {
        return carritos.findTopByUsuarioIdOrderByFechaCreacionDesc(usuarioId)
            .orElseGet(() -> crearCarrito(usuarioId));
    }

    @Transactional(readOnly = true)
    public List<Carrito> listarPorUsuario(String usuarioId) {
        return carritos.findByUsuarioId(usuarioId);
    }

    @Transactional
    public ItemCarrito agregarItem(Long carritoId, Long productoId) {
        Carrito carrito = obtenerPorId(carritoId);
        // El catálogo confirma que la skin sigue activa antes de agregarla.
        ProductoRemoto producto = catalogo.buscarProducto(productoId);
        if (producto == null) {
            throw new EntityNotFoundException("Producto no encontrado en catálogo");
        }
        if (!"ACTIVO".equalsIgnoreCase(producto.estado())) {
            throw new IllegalStateException("El producto no está disponible para la venta");
        }

        // El carrito admite una unidad por publicación y excluye las del propio comprador.
        if (producto.vendedorId() != null && producto.vendedorId().equals(carrito.getUsuarioId())) throw new IllegalStateException("No puedes agregar tu propia publicación");
        boolean yaExiste = carrito.getItems().stream()
            .anyMatch(item -> Objects.equals(item.getProductoId(), producto.id()));
        if (yaExiste) {
            throw new IllegalStateException(
                "La skin ya está en el carrito; solo se permite una unidad"
            );
        }

        ItemCarrito item = new ItemCarrito();
        item.setProductoId(producto.id());
        // Se guarda el nombre y el precio que tenía la skin al agregarla.
        item.setNombreItem(producto.nombre());
        item.setPrecio(producto.precio());
        item.setCantidad(1);
        item.setCarrito(carrito);
        carrito.getItems().add(item);
        carritos.save(carrito);
        return item;
    }

    @Transactional
    public void quitarItem(Long carritoId, Long productoId) {
        Carrito carrito = obtenerPorId(carritoId);
        boolean eliminado = carrito.getItems()
            .removeIf(item -> Objects.equals(item.getProductoId(), productoId));
        if (!eliminado) {
            throw new EntityNotFoundException("La skin no está en el carrito");
        }
        carritos.save(carrito);
    }

    @Transactional
    // Se quitan los ítems y se conserva el carrito de la cuenta.
    public Carrito vaciar(Long carritoId) {
        Carrito carrito = obtenerPorId(carritoId);
        carrito.getItems().clear();
        return carritos.save(carrito);
    }

    @Transactional
    public void eliminar(Long carritoId) {
        Carrito carrito = obtenerPorId(carritoId);
        carritos.delete(carrito);
    }

    @Transactional(readOnly = true)
    public BigDecimal subtotal(Long carritoId) {
        return obtenerPorId(carritoId).getItems().stream()
            .map(item -> item.getPrecio().multiply(BigDecimal.valueOf(item.getCantidad())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
