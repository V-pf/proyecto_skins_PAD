package cl.skinspad.carrito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import cl.skinspad.carrito.client.CatalogoClient;
import cl.skinspad.carrito.client.ProductoRemoto;
import cl.skinspad.carrito.domain.Carrito;
import cl.skinspad.carrito.domain.ItemCarrito;
import cl.skinspad.carrito.repository.CarritoRepository;
import cl.skinspad.carrito.service.CarritoService;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CarritoServiceTest {

    @Mock
    private CarritoRepository carritos;

    @Mock
    private CatalogoClient catalogo;

    @InjectMocks
    private CarritoService service;

    private Carrito carrito;

    @BeforeEach
    void preparar() {
        carrito = new Carrito();
        carrito.setId(10L);
        carrito.setUsuarioId("u1");
        carrito.setItems(new ArrayList<>());
        when(carritos.findById(10L)).thenReturn(Optional.of(carrito));
    }

    @Test
    void permiteAgregarUnProductoDesdeElServicio() {
        when(catalogo.buscarProducto(7L)).thenReturn(
            new ProductoRemoto(7L, "Skin", BigDecimal.TEN, "ACTIVO")
        );
        when(carritos.save(carrito)).thenReturn(carrito);

        service.agregarItem(10L, 7L);

        assertEquals(1, carrito.getItems().size());
        assertEquals(7L, carrito.getItems().get(0).getProductoId());
    }

    @Test
    void rechazaProductoDuplicadoDesdeElServicio() {
        when(catalogo.buscarProducto(7L)).thenReturn(
            new ProductoRemoto(7L, "Skin", BigDecimal.TEN, "ACTIVO")
        );
        ItemCarrito existente = new ItemCarrito();
        existente.setProductoId(7L);
        existente.setNombreItem("Skin");
        existente.setPrecio(BigDecimal.TEN);
        existente.setCarrito(carrito);
        carrito.getItems().add(existente);

        assertThrows(IllegalStateException.class, () -> service.agregarItem(10L, 7L));
    }

    @Test
    void quitarProductoEliminaElItemDesdeElServicio() {
        ItemCarrito existente = new ItemCarrito();
        existente.setProductoId(7L);
        existente.setCarrito(carrito);
        carrito.getItems().add(existente);
        when(carritos.save(carrito)).thenReturn(carrito);

        service.quitarItem(10L, 7L);

        assertEquals(0, carrito.getItems().size());
    }

    @Test
    void quitarProductoInexistenteLanzaError() {
        assertThrows(EntityNotFoundException.class, () -> service.quitarItem(10L, 99L));
    }
}
