package cl.skinspad.catalogo.controller;

import cl.skinspad.catalogo.domain.*;
import cl.skinspad.catalogo.dto.*;
import cl.skinspad.catalogo.service.CatalogoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/productos")
@RequiredArgsConstructor
public class ProductoController {
    private final CatalogoService service;

    @PostMapping("/reservas")
    public List<Producto> reservar(@Valid @RequestBody ReservaRequest r){return service.reservar(r);}

    @GetMapping
    public List<Producto> listar(@RequestParam(required = false) String q, @RequestParam(required = false) Long categoriaId,
        @RequestParam(required = false) String tipo, @RequestParam(required = false) BigDecimal precioMin, @RequestParam(required = false) BigDecimal precioMax,
        @RequestParam(required = false) EstadoProducto estado, @RequestParam(required = false) String vendedorId) {
        return service.buscarConFiltros(q, categoriaId, tipo, precioMin, precioMax, estado, vendedorId);
    }

    @GetMapping("/{id}")
    public Producto porId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @GetMapping("/vendedor/{vendedorId}")
    public List<Producto> porVendedor(@PathVariable String vendedorId) {
        return service.buscarPorVendedor(vendedorId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Producto crear(@Valid @RequestBody ProductoRequest r) {
        return service.crearProducto(r);
    }

    @PutMapping("/{id}")
    public Producto actualizar(@PathVariable Long id, @Valid @RequestBody ProductoRequest r) {
        return service.actualizarProducto(id, r);
    }

    @PatchMapping("/{id}/precio")
    public Producto precio(@PathVariable Long id, @Valid @RequestBody PrecioRequest r) {
        return service.actualizarPrecio(id, r.nuevoPrecio());
    }

    @PatchMapping("/{id}/estado")
    public Producto estado(@PathVariable Long id, @Valid @RequestBody EstadoRequest r) {
        return service.actualizarEstado(id, r.estado());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id, @RequestParam(required = false) String motivo) {
        service.eliminarProducto(id, motivo);
    }

    @PostMapping("/{id}/imagenes")
    @ResponseStatus(HttpStatus.CREATED)
    public ImagenProducto agregarImagen(@PathVariable Long id, @Valid @RequestBody ImagenRequest r) {
        return service.agregarImagen(id, r);
    }

    @DeleteMapping("/{id}/imagenes/{imagenId}")
    public void quitarDeProducto(@PathVariable Long id,@PathVariable Long imagenId){service.quitarImagen(id,imagenId);}

    @DeleteMapping("/imagenes/{imagenId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void quitarImagen(@PathVariable Long imagenId) {
        service.quitarImagen(imagenId);
    }
}
