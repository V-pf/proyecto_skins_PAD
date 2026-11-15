package cl.skinspad.catalogo.service;

import cl.skinspad.catalogo.domain.Categoria;
import cl.skinspad.catalogo.dto.CategoriaRequest;
import cl.skinspad.catalogo.repository.CategoriaRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CategoriaService {

    private final CategoriaRepository repository;
    private final cl.skinspad.catalogo.repository.ProductoRepository productos;

    @Transactional(readOnly = true)
    public List<Categoria> listar() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Categoria buscar(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Categoría no encontrada: " + id));
    }

    @Transactional
    public Categoria crear(CategoriaRequest request) {
        // Se ordenan los espacios del nombre antes de buscar duplicados.
        String nombre = request.nombre().strip().replaceAll("\\s+", " ");
        if (repository.findByNombreIgnoreCase(nombre).isPresent()) {
            throw new IllegalArgumentException("Ya existe una categoría con ese nombre");
        }

        Categoria categoria = new Categoria(null, nombre, request.descripcion(), true);
        return repository.save(categoria);
    }

    @Transactional
    public Categoria editar(Long id, CategoriaRequest request) {
        Categoria categoria = buscar(id);
        String nombre = request.nombre().strip().replaceAll("\\s+", " ");

        // Al editar, la categoría puede conservar su propio nombre.
        repository.findByNombreIgnoreCase(nombre)
            .filter(otra -> !otra.getId().equals(id))
            .ifPresent(otra -> {
                throw new IllegalArgumentException("Ya existe una categoría con ese nombre");
        });

        categoria.setNombre(nombre);
        categoria.setDescripcion(request.descripcion());
        return repository.save(categoria);
    }

    @Transactional
    public void eliminar(Long id) {
        Categoria categoria = buscar(id);
        // Una categoría con publicaciones asociadas se puede deshabilitar para conservar ese vínculo.
        if (productos.existsByCategoria_Id(id)) throw new IllegalStateException("La categoría tiene publicaciones asociadas. Puedes renombrarla o deshabilitarla.");
        repository.delete(categoria);
    }

    @Transactional
    public Categoria deshabilitar(Long id) {
        Categoria categoria = buscar(id);
        categoria.setEstadoHabilitada(false);
        return repository.save(categoria);
    }
}
