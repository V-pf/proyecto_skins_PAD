package cl.skinspad.carrito.controller;

import cl.skinspad.carrito.domain.*;
import cl.skinspad.carrito.dto.AgregarItemRequest;
import cl.skinspad.carrito.service.CarritoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/carritos")
@RequiredArgsConstructor
public class CarritoController {
    private final CarritoService service;

    @PostMapping("/usuario/{usuarioId}")
    @ResponseStatus(HttpStatus.CREATED)
    public Carrito crear(@PathVariable String usuarioId) {
        return service.crearCarrito(usuarioId);
    }

    @GetMapping("/{id}")
    public Carrito porId(@PathVariable Long id) {
        return service.obtenerPorId(id);
    }

    @GetMapping("/usuario/{usuarioId}")
    public Carrito porUsuario(@PathVariable String usuarioId) {
        return service.obtenerPorUsuario(usuarioId);
    }

    @GetMapping("/usuario/{usuarioId}/todos")
    public List<Carrito> todos(@PathVariable String usuarioId) {
        return service.listarPorUsuario(usuarioId);
    }

    @GetMapping("/{id}/subtotal")
    public BigDecimal subtotal(@PathVariable Long id) {
        return service.subtotal(id);
    }

    @PostMapping("/{id}/items")
    @ResponseStatus(HttpStatus.CREATED)
    public ItemCarrito agregar(@PathVariable Long id, @Valid @RequestBody AgregarItemRequest r) {
        return service.agregarItem(id, r.productoId());
    }

    @DeleteMapping("/{id}/items/{productoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void quitar(@PathVariable Long id, @PathVariable Long productoId) {
        service.quitarItem(id, productoId);
    }

    @DeleteMapping("/{id}/items")
    public Carrito vaciar(@PathVariable Long id) {
        return service.vaciar(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        service.eliminar(id);
    }
}
