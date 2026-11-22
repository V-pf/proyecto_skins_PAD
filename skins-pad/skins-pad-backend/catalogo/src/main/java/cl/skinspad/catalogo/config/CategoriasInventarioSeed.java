package cl.skinspad.catalogo.config;

import cl.skinspad.catalogo.domain.Categoria;
import cl.skinspad.catalogo.repository.CategoriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(100)
@RequiredArgsConstructor
public class CategoriasInventarioSeed implements CommandLineRunner {
  private final CategoriaRepository categorias;

  public void run(String... args) {
    for (String nombre : java.util.List.of("Escopetas", "Ametralladoras")) {
      if (categorias.findAll().stream().noneMatch(c -> nombre.equalsIgnoreCase(c.getNombre())))
        categorias.save(new Categoria(null, nombre, "Skins de " + nombre.toLowerCase(), true));
    }
  }
}
