package cl.skinspad.catalogo.controller;

import cl.skinspad.catalogo.domain.Categoria;
import cl.skinspad.catalogo.dto.CategoriaRequest;
import cl.skinspad.catalogo.service.CategoriaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/categorias")
@RequiredArgsConstructor
public class CategoriaController {
    private final CategoriaService service;

    @GetMapping
    public List<Categoria> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public Categoria buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Categoria crear(@Valid @RequestBody CategoriaRequest r) {
        return service.crear(r);
    }

    @PutMapping("/{id}")
    public Categoria editar(@PathVariable Long id, @Valid @RequestBody CategoriaRequest r) {
        return service.editar(id, r);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id){service.eliminar(id);}

    @PatchMapping("/{id}/deshabilitar")
    public Categoria deshabilitar(@PathVariable Long id) {
        return service.deshabilitar(id);
    }
}
