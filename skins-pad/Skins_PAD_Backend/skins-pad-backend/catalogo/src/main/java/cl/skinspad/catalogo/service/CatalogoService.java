package cl.skinspad.catalogo.service;

import cl.skinspad.catalogo.domain.Categoria;
import cl.skinspad.catalogo.domain.EstadoProducto;
import cl.skinspad.catalogo.domain.ImagenProducto;
import cl.skinspad.catalogo.domain.Producto;
import cl.skinspad.catalogo.dto.ImagenRequest;
import cl.skinspad.catalogo.dto.ProductoRequest;
import cl.skinspad.catalogo.repository.CategoriaRepository;
import cl.skinspad.catalogo.repository.ImagenProductoRepository;
import cl.skinspad.catalogo.repository.ProductoRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CatalogoService {

    private final ProductoRepository productos;
    private final CategoriaRepository categorias;
    private final ImagenProductoRepository imagenes;

    @Transactional(readOnly = true)
    public List<Producto> buscarConFiltros(
        String q,
        Long categoriaId,
        String tipo,
        BigDecimal min,
        BigDecimal max,
        EstadoProducto estado,
        String vendedorId
    ) {
        String needle = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        return productos.findAll().stream()
            .filter(producto -> needle.isEmpty()
            || producto.getNombre().toLowerCase(Locale.ROOT).contains(needle)
            || (producto.getDescripcion() != null
            && producto.getDescripcion().toLowerCase(Locale.ROOT).contains(needle)))
            .filter(producto -> categoriaId == null
            || (producto.getCategoria() != null
            && Objects.equals(producto.getCategoria().getId(), categoriaId)))
            .filter(producto -> tipo == null || tipo.isBlank()
            || (producto.getTipo() != null && producto.getTipo().equalsIgnoreCase(tipo)))
            .filter(producto -> min == null || producto.getPrecio().compareTo(min) >= 0)
            .filter(producto -> max == null || producto.getPrecio().compareTo(max) <= 0)
            .filter(producto -> estado == null || producto.getEstado() == estado)
            .filter(producto -> vendedorId == null
            || Objects.equals(producto.getVendedorId(), vendedorId))
            .toList();
    }

    @Transactional(readOnly = true)
    public Producto buscarPorId(Long id) {
        return productos.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado: " + id));
    }

    @Transactional(readOnly = true)
    public List<Producto> buscarPorVendedor(String vendedorId) {
        return productos.findByVendedorId(vendedorId);
    }

    @Transactional
    public Producto crearProducto(ProductoRequest request) {
        Categoria categoria = buscarCategoria(request.categoriaId());
        Producto producto = new Producto();
        producto.setFechaCreacion(LocalDateTime.now());
        aplicarDatosProducto(producto, request, categoria);
        if (request.estado() == null) {
            producto.setEstado(EstadoProducto.PENDIENTE_APROBACION);
        }
        return productos.save(producto);
    }

    @Transactional
    public Producto actualizarProducto(Long id, ProductoRequest request) {
        Producto producto = productos.bloquear(id).orElseThrow(()->new EntityNotFoundException("Publicación inexistente"));
        if(producto.getReservaPago()!=null) throw new IllegalStateException("Esta skin está reservada o vendida");
        Categoria categoria = buscarCategoria(request.categoriaId());
        aplicarDatosProducto(producto, request, categoria);
        return productos.save(producto);
    }

    private Categoria buscarCategoria(Long categoriaId) {
        return categorias.findById(categoriaId)
            .orElseThrow(() -> new EntityNotFoundException("Categoría no encontrada: " + categoriaId));
    }

    private void aplicarDatosProducto(
        Producto producto,
        ProductoRequest request,
        Categoria categoria
    ) {
        if (!categoria.isEstadoHabilitada()) {
            throw new IllegalStateException(
                "No se pueden publicar productos en una categoría deshabilitada"
            );
        }

        producto.setVendedorId(request.vendedorId());
        producto.setVendedor(request.vendedor());
        producto.setCategoria(categoria);
        producto.setNombre(request.nombre().trim());
        producto.setDescripcion(request.descripcion());
        producto.setPrecio(request.precio());
        if (request.estado() != null) {
            producto.setEstado(request.estado());
        }
        producto.setTipo(request.tipo());
        producto.setGrado(request.grado());
        producto.setDesgaste(request.desgaste());
        producto.setStattrak(Boolean.TRUE.equals(request.stattrak()));
        producto.setFloatValor(request.floatValor());
        producto.setMotivoRechazo(request.motivoRechazo());
        producto.setUrlIntercambio(request.urlIntercambio());
    }

    @Transactional
    public List<Producto> reservar(cl.skinspad.catalogo.dto.ReservaRequest r) {
        var lista = r.ids().stream().distinct().sorted().map(id->productos.bloquear(id)
            .orElseThrow(()->new EntityNotFoundException("Skin inexistente"))).toList();
        for(var p:lista) {
            switch(r.accion()) {
                case "COTIZAR" -> {
                    if(p.getEstado()!=EstadoProducto.ACTIVO || p.getReservaPago()!=null)throw new IllegalStateException("Skin no disponible");
                }
                case "DESCARTAR" -> {
                    if(r.referencia().equals(p.getReservaPago()) && p.getEstado()==EstadoProducto.INACTIVO){
                        p.setReservaPago(null);p.setEstado(EstadoProducto.ACTIVO);
                    }
                }
                case "RESERVAR" -> {
                    if(r.referencia().equals(p.getReservaPago())) continue;
                    if(p.getReservaPago()!=null || p.getEstado()!=EstadoProducto.ACTIVO) throw new IllegalStateException("Skin no disponible");
                    p.setReservaPago(r.referencia());p.setEstado(EstadoProducto.INACTIVO);
                }
                case "VENDER","LIBERAR" -> {
                    if("LIBERAR".equals(r.accion()) && p.getReservaPago()==null && p.getEstado()==EstadoProducto.ACTIVO) continue;
                    if(!r.referencia().equals(p.getReservaPago())) throw new IllegalStateException("Reserva ajena");
                    if("VENDER".equals(r.accion())) p.setEstado(EstadoProducto.VENDIDO);
                    else {p.setReservaPago(null);p.setEstado(EstadoProducto.ACTIVO);}
                }
                default -> throw new IllegalArgumentException("Acción inválida");
            }
        }
        return productos.saveAll(lista);
    }

    @Transactional
    public Producto actualizarPrecio(Long id, BigDecimal precio) {
        if (precio == null || precio.signum() <= 0) {
            throw new IllegalArgumentException("El precio debe ser mayor que cero");
        }
        Producto producto = buscarPorId(id);
        if(producto.getEstado()!=EstadoProducto.ACTIVO || producto.getReservaPago()!=null)throw new IllegalStateException("La publicación debe estar disponible");
        producto.setPrecio(precio);
        return productos.save(producto);
    }

    @Transactional
    public Producto actualizarEstado(Long id, EstadoProducto estado) {
        if (estado == null) {
            throw new IllegalArgumentException("Estado obligatorio");
        }
        Producto producto = buscarPorId(id);
        producto.setEstado(estado);
        return productos.save(producto);
    }

    @Transactional
    public ImagenProducto agregarImagen(Long productoId, ImagenRequest request) {
        Producto producto = buscarPorId(productoId);
        String url=request.urlImagen();
        if (!url.matches("data:image/(png|jpeg);base64,[A-Za-z0-9+/=]+"))
            throw new IllegalArgumentException("Usa una imagen PNG o JPEG");
        byte[] bytes=java.util.Base64.getDecoder().decode(url.substring(url.indexOf(',')+1));
        boolean jpeg=bytes.length>3 && bytes[0]==(byte)0xff && bytes[1]==(byte)0xd8 && bytes[2]==(byte)0xff;
        boolean png=bytes.length>8 && java.nio.ByteBuffer.wrap(bytes).getLong()==0x89504e470d0a1a0aL;
        if(bytes.length>32768 || !(url.startsWith("data:image/png;")?png:jpeg))
            throw new IllegalArgumentException("Formato o tamaño de imagen inválido");
        if (request.esPrincipal()) {
            producto.getImagenes().forEach(imagen -> imagen.setEsPrincipal(false));
        }

        ImagenProducto imagen = new ImagenProducto();
        imagen.setUrlImagen(request.urlImagen());
        imagen.setEsPrincipal(request.esPrincipal());
        imagen.setProducto(producto);
        producto.getImagenes().add(imagen);
        productos.save(producto);
        return imagen;
    }

    @Transactional
    public void quitarImagen(Long productoId, Long imagenId) {
        var imagen=imagenes.findById(imagenId).orElseThrow();
        if(!productoId.equals(imagen.getProducto().getId()))throw new IllegalArgumentException("Imagen ajena");
        imagenes.delete(imagen);
    }

    @Transactional
    public void quitarImagen(Long imagenId) {
        if (!imagenes.existsById(imagenId)) {
            throw new EntityNotFoundException("Imagen no encontrada: " + imagenId);
        }
        imagenes.deleteById(imagenId);
    }

    @Transactional
    public void eliminarProducto(Long id, String motivo) {
        Producto producto = buscarPorId(id);
        String descripcionActual = producto.getDescripcion() == null ? "" : producto.getDescripcion() + "\n";
        String razon = motivo == null || motivo.isBlank() ? "sin motivo especificado" : motivo;
        producto.setDescripcion(descripcionActual + "Retirado: " + razon);
        producto.setEstado(EstadoProducto.ELIMINADO);
        productos.save(producto);
    }
}
