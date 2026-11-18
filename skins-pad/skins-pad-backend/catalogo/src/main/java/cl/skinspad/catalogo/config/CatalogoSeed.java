package cl.skinspad.catalogo.config;

import cl.skinspad.catalogo.domain.*;
import cl.skinspad.catalogo.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.math.BigDecimal;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class CatalogoSeed {

    @Bean @org.springframework.core.annotation.Order(0) CommandLineRunner cargarDatos(CategoriaRepository categorias, ProductoRepository productos) {
        return args -> {
            if(categorias.count()>0 || productos.count()>0) return;
            Map<String, String> descripciones = Map.of("rifle", "Rifles", "francotirador", "Rifles de francotirador", "pistola", "Pistolas", "subfusil", "Subfusiles", "cuchillo", "Cuchillos", "guantes", "Guantes", "sticker", "Stickers");
            Map<String, Categoria> cats = new java.util.HashMap<>();
            for(var e:descripciones.entrySet()) cats.put(e.getKey(), categorias.save(new Categoria(null, e.getValue(), "Categoría de skins: " + e.getKey(), true)));
            agregar(productos, cats, "u3", "AK-47 | Redline", "rifle", "clasificado", "ft", 28990, false, "0.2461", "PenquistaSkins", EstadoProducto.ACTIVO);
            agregar(productos, cats, "u4", "AWP | Asiimov", "francotirador", "encubierto", "ft", 64990, true, "0.2874", "BioBioSkins", EstadoProducto.ACTIVO);
            agregar(productos, cats, "u4", "M4A4 | Howl", "rifle", "contrabando", "mw", 3950000, true, "0.0912", "BioBioSkins", EstadoProducto.ACTIVO);
            agregar(productos, cats, "u3", "USP-S | Kill Confirmed", "pistola", "encubierto", "fn", 42500, true, "0.0512", "PenquistaSkins", EstadoProducto.ACTIVO);
            agregar(productos, cats, "u4", "Glock-18 | Fade", "pistola", "restringido", "fn", 189990, false, "0.0298", "BioBioSkins", EstadoProducto.ACTIVO);
            agregar(productos, cats, "u4", "★ Karambit | Doppler", "cuchillo", "especial", "fn", 1290000, false, "0.0121", "BioBioSkins", EstadoProducto.ACTIVO);
            agregar(productos, cats, "u3", "★ Sport Gloves | Vice", "guantes", "especial", "ft", 2150000, false, "0.2203", "PenquistaSkins", EstadoProducto.ACTIVO);
            agregar(productos, cats, "u3", "P250 | Sand Dune", "pistola", "consumidor", "bs", 190, false, "0.5710", "PenquistaSkins", EstadoProducto.ACTIVO);
            agregar(productos, cats, "u3", "M4A1-S | Cyrex", "rifle", "clasificado", "mw", 17990, false, "0.1024", "PenquistaSkins", EstadoProducto.ACTIVO);
            agregar(productos, cats, "u4", "MP9 | Hot Rod", "subfusil", "clasificado", "fn", 8990, false, "0.0455", "BioBioSkins", EstadoProducto.ACTIVO);
            agregar(productos, cats, "u4", "Sticker | Crown (Foil)", "sticker", "contrabando", "na", 1590000, false, null, "BioBioSkins", EstadoProducto.ACTIVO);
            agregar(productos, cats, "u3", "Desert Eagle | Blaze", "pistola", "restringido", "fn", 389990, false, "0.0103", "PenquistaSkins", EstadoProducto.ACTIVO);
            agregar(productos, cats, "u6", "★ Karambit | Doppler", "cuchillo", "especial", "fn", 1290000, false, "0.0135", "HualpenTrade", EstadoProducto.PENDIENTE_APROBACION);
            agregar(productos, cats, "u7", "M4A4 | Howl", "rifle", "contrabando", "mw", 3950000, false, "0.1101", "LotaLoot", EstadoProducto.PENDIENTE_APROBACION);
            agregar(productos, cats, "u8", "P250 | Sand Dune", "pistola", "consumidor", "bs", 190, false, "0.6012", "CoronelDrops", EstadoProducto.RECHAZADO);
        };
    }

    private void agregar(ProductoRepository repo, Map<String, Categoria> cats, String vendedorId, String nombre, String tipo, String grado, String desgaste, int precio, boolean stattrak, String floatV, String vendedor, EstadoProducto estado) {
        Producto p = new Producto();
        p.setVendedorId(vendedorId);
        p.setVendedor(vendedor);
        p.setCategoria(cats.get(tipo));
        p.setNombre(nombre);
        p.setTipo(tipo);
        p.setGrado(grado);
        p.setDesgaste(desgaste);
        p.setPrecio(BigDecimal.valueOf(precio));
        p.setStattrak(stattrak);
        p.setFloatValor(floatV == null?null:new BigDecimal(floatV));
        p.setEstado(estado);
        p.setDescripcion("Skin publicada en Skins PAD");
        repo.save(p);
    }
}
