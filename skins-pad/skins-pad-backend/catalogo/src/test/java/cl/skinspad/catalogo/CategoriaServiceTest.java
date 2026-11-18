package cl.skinspad.catalogo;

import cl.skinspad.catalogo.domain.Categoria;
import cl.skinspad.catalogo.dto.CategoriaRequest;
import cl.skinspad.catalogo.repository.CategoriaRepository;
import cl.skinspad.catalogo.repository.ProductoRepository;
import cl.skinspad.catalogo.service.CategoriaService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class CategoriaServiceTest {
    private final CategoriaRepository categorias = mock(CategoriaRepository.class);
    private final ProductoRepository productos = mock(ProductoRepository.class);
    private final CategoriaService servicio = new CategoriaService(categorias, productos);
    private final Categoria rifle = new Categoria(1L, "Rifles de asalto", "", true);

    @Test void rechazaNombreDuplicadoConEspacios() {
        when(categorias.findByNombreIgnoreCase("Rifles de asalto")).thenReturn(Optional.of(rifle));
        assertThrows(IllegalArgumentException.class,
            () -> servicio.crear(new CategoriaRequest(" Rifles   de asalto ", "")));
        verify(categorias, never()).save(any());
    }

    @Test void conservaCategoriaConPublicaciones() {
        when(categorias.findById(1L)).thenReturn(Optional.of(rifle));
        when(productos.existsByCategoria_Id(1L)).thenReturn(true);
        assertThrows(IllegalStateException.class, () -> servicio.eliminar(1L));
        verify(categorias, never()).delete(any());
    }

    @Test void eliminaCategoriaSinPublicaciones() {
        when(categorias.findById(1L)).thenReturn(Optional.of(rifle));
        servicio.eliminar(1L);
        verify(categorias).delete(rifle);
    }
}
