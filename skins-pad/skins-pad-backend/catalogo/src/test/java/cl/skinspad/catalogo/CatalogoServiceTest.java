package cl.skinspad.catalogo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import cl.skinspad.catalogo.domain.EstadoProducto;
import cl.skinspad.catalogo.domain.Producto;
import cl.skinspad.catalogo.repository.CategoriaRepository;
import cl.skinspad.catalogo.repository.ImagenProductoRepository;
import cl.skinspad.catalogo.repository.ProductoRepository;
import cl.skinspad.catalogo.service.CatalogoService;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CatalogoServiceTest {

    @Mock
    private ProductoRepository productos;

    @Mock
    private CategoriaRepository categorias;

    @Mock
    private ImagenProductoRepository imagenes;

    @InjectMocks
    private CatalogoService service;

    private Producto producto;

    @BeforeEach
    void preparar() {
        producto = new Producto();
        producto.setId(1L);
        producto.setPrecio(new BigDecimal("1000"));
        producto.setEstado(EstadoProducto.ACTIVO);
    }

    @Test
    void actualizaPrecioValidoDesdeElServicio() {
        when(productos.bloquear(1L)).thenReturn(Optional.of(producto));
        when(productos.save(producto)).thenReturn(producto);

        Producto resultado = service.actualizarPrecio(1L, new BigDecimal("2500"));

        assertEquals(0, new BigDecimal("2500").compareTo(resultado.getPrecio()));
    }

    @Test
    void rechazaPrecioNoPositivoDesdeElServicio() {
        assertThrows(
            IllegalArgumentException.class,
            () -> service.actualizarPrecio(1L, BigDecimal.ZERO)
        );
    }

    @Test
    void eliminarProductoCambiaElEstadoDesdeElServicio() {
        when(productos.bloquear(1L)).thenReturn(Optional.of(producto));
        when(productos.save(producto)).thenReturn(producto);

        service.eliminarProducto(1L, "Prueba docente", false);

        assertEquals(EstadoProducto.ELIMINADO, producto.getEstado());
        assertEquals("Prueba docente", producto.getMotivoRechazo());
    }
    @Test
    void noRetiraUnaPublicacionReservada() {
        producto.setReservaPago("compra-1");
        when(productos.bloquear(1L)).thenReturn(Optional.of(producto));
        assertThrows(IllegalStateException.class, () -> service.eliminarProducto(1L, "Retiro", false));
        assertEquals(EstadoProducto.ACTIVO, producto.getEstado());
    }
    @Test
    void noRetiraUnaSkinVendida() {
        producto.setEstado(EstadoProducto.VENDIDO);
        when(productos.bloquear(1L)).thenReturn(Optional.of(producto));
        assertThrows(IllegalStateException.class, () -> service.eliminarProducto(1L, "Retiro", false));
        assertEquals(EstadoProducto.VENDIDO, producto.getEstado());
    }
}
