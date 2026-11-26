package cl.skinspad.inventario.repository;

import cl.skinspad.inventario.domain.ItemInventario;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemInventarioRepository extends JpaRepository<ItemInventario, Long> {
  List<ItemInventario> findByUsuarioIdOrderById(String usuarioId);

  Optional<ItemInventario> findByUsuarioIdAndAssetId(String usuarioId, String assetId);

  Optional<ItemInventario> findFirstByUsuarioIdAndAssetIdStartingWithOrderById(String usuarioId, String prefijo);
}
