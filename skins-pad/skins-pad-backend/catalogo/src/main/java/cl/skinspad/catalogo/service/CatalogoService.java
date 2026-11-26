package cl.skinspad.catalogo.service;

import cl.skinspad.catalogo.client.InventarioAuth;
import cl.skinspad.catalogo.client.InventarioClient;
import cl.skinspad.catalogo.domain.Categoria;
import cl.skinspad.catalogo.domain.EstadoProducto;
import cl.skinspad.catalogo.domain.ImagenProducto;
import cl.skinspad.catalogo.domain.Producto;
import cl.skinspad.catalogo.dto.EstadoPublicacionInventario;
import cl.skinspad.catalogo.dto.ImagenRequest;
import cl.skinspad.catalogo.dto.ProductoRequest;
import cl.skinspad.catalogo.repository.CategoriaRepository;
import cl.skinspad.catalogo.repository.ImagenProductoRepository;
import cl.skinspad.catalogo.repository.ProductoRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CatalogoService {


    private final ProductoRepository productos;
    private final CategoriaRepository categorias;
    private final ImagenProductoRepository imagenes;
    private final InventarioClient inventario;

    @Transactional(readOnly = true)
    // La búsqueda aplica solo los filtros que llegaron en la consulta.
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
        // Si el formulario no trae estado, la publicación queda en revisión.
        if (request.estado() == null) {
            producto.setEstado(EstadoProducto.PENDIENTE_APROBACION);
        }
        return productos.save(producto);
    }

    @Transactional
    public Producto actualizarProducto(Long id, ProductoRequest request) {
        // Se bloquea la publicación al editar para respetar la reserva de una compra.
        Producto producto = productos.bloquear(id).orElseThrow(()->new EntityNotFoundException("Publicación inexistente"));
        if(producto.getReservaPago()!=null) throw new IllegalStateException("Esta skin está reservada o vendida");
        if (producto.getInventarioId() != null) {
            if (producto.getEstado() != EstadoProducto.ACTIVO && producto.getEstado() != EstadoProducto.PENDIENTE_APROBACION) {
                throw new IllegalStateException("Solo se pueden editar publicaciones activas o pendientes.");
            }
            producto.setPrecio(request.precio());
            if(request.estado()==EstadoProducto.ELIMINADO) producto.setEstado(EstadoProducto.ELIMINADO);
            return productos.save(producto);
        }
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
        // La misma regla de categoría se aplica al crear y al editar.
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
    }

    @Transactional
    public List<Producto> reservar(cl.skinspad.catalogo.dto.ReservaRequest r) {
        // Se bloquean las skins en orden de ID para que las compras simultáneas sigan el mismo orden.
        var lista = r.ids().stream().distinct().sorted().map(id->productos.bloquear(id)
            .orElseThrow(()->new EntityNotFoundException("Skin inexistente"))).toList();
        for(var p:lista) {
            // Cotizar comprueba la disponibilidad. Reservar aparta la skin para una compra.
            // La referencia permite identificar qué pago puede venderla o devolverla al catálogo.
            switch(r.accion()) {
                case "COTIZAR" -> {
                    verificarDisponibleParaCompra(p);
                }
                case "DESCARTAR" -> {
                    if (r.referencia().equals(p.getReservaPago())
                            && (p.getEstado() == EstadoProducto.INACTIVO || p.getEstado() == EstadoProducto.ELIMINADO)) {
                        liberarReserva(p);
                    }
                }
                case "RESERVAR" -> {
                    if(r.referencia().equals(p.getReservaPago())) continue;
                    verificarDisponibleParaCompra(p);
                    p.setReservaPago(r.referencia());p.setEstado(EstadoProducto.INACTIVO);
                }
                case "VENDER","LIBERAR" -> {
                    if ("LIBERAR".equals(r.accion()) && p.getReservaPago() == null
                        && p.getEstado() == estadoAlLiberar(p)) continue;
                    if(!r.referencia().equals(p.getReservaPago())) throw new IllegalStateException("Reserva ajena");
                    if("VENDER".equals(r.accion())) p.setEstado(EstadoProducto.VENDIDO);
                    else liberarReserva(p);
                }
                default -> throw new IllegalArgumentException("Acción inválida");
            }
        }
        return productos.saveAll(lista);
    }

    private void liberarReserva(Producto producto) {
        producto.setReservaPago(null);
        producto.setEstado(estadoAlLiberar(producto));
    }

    private EstadoProducto estadoAlLiberar(Producto producto) {
        return producto.getEstado() == EstadoProducto.ELIMINADO
            ? EstadoProducto.ELIMINADO : EstadoProducto.ACTIVO;
    }

    @Transactional
    public Producto actualizarPrecio(Long id, BigDecimal precio) {
        if (precio == null || precio.signum() <= 0) {
            throw new IllegalArgumentException("El precio debe ser mayor que cero");
        }
        Producto producto = productos.bloquear(id).orElseThrow();
        if(producto.getEstado()!=EstadoProducto.ACTIVO || producto.getReservaPago()!=null)throw new IllegalStateException("La publicación debe estar disponible");
        producto.setPrecio(precio);
        return productos.save(producto);
    }

    @Transactional
    public Producto actualizarEstado(Long id, EstadoProducto estado) {
        return actualizarEstado(id, estado, null);
    }

    @Transactional
    public Producto actualizarEstado(Long id, EstadoProducto estado, String motivoRechazo) {
        if (estado != EstadoProducto.ACTIVO && estado != EstadoProducto.RECHAZADO) {
            throw new IllegalArgumentException("Indica si la publicación es apta o no apta.");
        }
        Producto producto = productos.bloquear(id).orElseThrow();
        if (producto.getReservaPago() != null || producto.getEstado() == EstadoProducto.VENDIDO) {
            throw new IllegalStateException("La publicación ya está reservada o vendida.");
        }
        if (estado == EstadoProducto.ACTIVO) {
            if (producto.getEstado() != EstadoProducto.ACTIVO
                    && !Boolean.FALSE.equals(producto.getRevisionApta())) {
                throw new IllegalStateException("La publicación no está disponible para revisión.");
            }
            producto.setRevisionApta(true);
            producto.setMotivoRechazo(null);
        } else {
            if (producto.getEstado() != EstadoProducto.ACTIVO) {
                throw new IllegalStateException("Solo se pueden retirar por moderación publicaciones activas.");
            }
            if (motivoRechazo == null || motivoRechazo.isBlank()) {
                throw new IllegalArgumentException("Indica el motivo del retiro.");
            }
            producto.setEstado(EstadoProducto.RECHAZADO);
            producto.setRevisionApta(false);
            producto.setMotivoRechazo(motivoRechazo.trim());
        }
        return productos.save(producto);
    }

    @Transactional
    public ImagenProducto agregarImagen(Long productoId, ImagenRequest request) {
        Producto producto = buscarPorId(productoId);
        if(producto.getInventarioId()!=null) throw new IllegalStateException("Las imágenes de esta publicación provienen del inventario.");
        // Además del tipo declarado, se revisan el tamaño y los primeros bytes del archivo.
        String url=request.urlImagen();
        if (!url.matches("data:image/(png|jpeg);base64,[A-Za-z0-9+/=]+"))
            throw new IllegalArgumentException("Usa una imagen PNG o JPEG");
        byte[] bytes=java.util.Base64.getDecoder().decode(url.substring(url.indexOf(',')+1));
        boolean jpeg=bytes.length>3 && bytes[0]==(byte)0xff && bytes[1]==(byte)0xd8 && bytes[2]==(byte)0xff;
        boolean png=bytes.length>8 && java.nio.ByteBuffer.wrap(bytes).getLong()==0x89504e470d0a1a0aL;
        if(bytes.length>32768 || !(url.startsWith("data:image/png;")?png:jpeg))
            throw new IllegalArgumentException("Formato o tamaño de imagen inválido");
        // Si esta foto será la principal, se desmarca la que estaba antes.
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
        if(imagen.getProducto().getInventarioId()!=null) throw new IllegalStateException("Las imágenes de esta publicación provienen del inventario.");
        if(!productoId.equals(imagen.getProducto().getId()))throw new IllegalArgumentException("Imagen ajena");
        imagenes.delete(imagen);
    }

    @Transactional
    public void quitarImagen(Long imagenId) {
        if (!imagenes.existsById(imagenId)) {
            throw new EntityNotFoundException("Imagen no encontrada: " + imagenId);
        }
        if(imagenes.findById(imagenId).orElseThrow().getProducto().getInventarioId()!=null) throw new IllegalStateException("Las imágenes de esta publicación provienen del inventario.");
        imagenes.deleteById(imagenId);
    }

    @Transactional
    public void eliminarProducto(Long id, String motivo, boolean porModeracion) {
        Producto producto = productos.bloquear(id).orElseThrow(() -> new EntityNotFoundException("Publicación inexistente"));
        if (producto.getReservaPago() != null || (producto.getEstado() != EstadoProducto.ACTIVO
            && producto.getEstado() != EstadoProducto.PENDIENTE_APROBACION)) {
            throw new IllegalStateException("Solo se pueden retirar publicaciones activas o pendientes sin reservas");
        }
        String razon = motivo == null || motivo.isBlank() ? "Retirada del catálogo" : motivo.trim();
        producto.setMotivoRechazo(razon);
        if (porModeracion) producto.setRevisionApta(false);
        producto.setEstado(EstadoProducto.ELIMINADO);
        productos.save(producto);
    }

    @Transactional(readOnly = true)
    public List<EstadoPublicacionInventario> estadosDeInventario(String usuarioId) {
        Map<Long, Producto> publicaciones = new LinkedHashMap<>();
        for (Producto p : productos.findByVendedorIdAndInventarioIdIsNotNullOrderById(usuarioId)) {
            publicaciones.merge(p.getInventarioId(), p,
                    (anterior, reciente) -> anterior.permiteRepublicar() ? reciente : anterior);
        }
        return publicaciones.values().stream()
                .map(p -> {
                    String estado = p.getReservaPago() != null && p.getEstado() != EstadoProducto.VENDIDO
                            ? "RESERVADO" : p.getEstado().name();
                    return new EstadoPublicacionInventario(p.getInventarioId(), p.getId(), estado, p.permiteRepublicar());
                })
                .toList();
    }

    private void verificarDisponibleParaCompra(Producto producto) {
        if (producto.getEstado() != EstadoProducto.ACTIVO || producto.getReservaPago() != null) {
            throw new IllegalStateException("Skin no disponible");
        }
        if (producto.getInventarioId() != null) {
            inventario.verificarDisponible(producto.getVendedorId(), producto.getInventarioId());
        }
    }

    @Transactional
    public Producto publicarDesdeInventario(InventarioAuth.Cuenta cuenta, Long itemId, BigDecimal precio) {
        if (!cuenta.verificado()) {
            throw new IllegalStateException(
                    "Tu cuenta debe ser verificada por administración antes de publicar.");
        }
        var item = inventario.verificarDisponible(cuenta.id(), itemId);
        if (productos.existsByInventarioOcupadoId(itemId)) {
            throw new IllegalStateException("El ítem no está disponible para publicar.");
        }
        Producto origen = item.productoOrigenId() == null
                ? null
                : productos.findById(item.productoOrigenId())
                        .filter(p -> itemId.equals(p.getInventarioId()) && cuenta.id().equals(p.getVendedorId()))
                        .orElseThrow(() -> new IllegalStateException("El traslado de este ítem sigue en curso."));
        Categoria categoria = origen == null ? categoriaPara(item.tipo()) : origen.getCategoria();
        if (!categoria.isEstadoHabilitada()) {
            throw new IllegalStateException("La categoría de este ítem no está habilitada.");
        }
        var p = new Producto();
        p.setCategoria(categoria);
        p.setNombre(item.nombre());
        p.setVendedorId(cuenta.id());
        p.setVendedor(cuenta.username());
        p.setTipo(item.tipo());
        p.setGrado(item.grado());
        p.setDesgaste(item.desgaste());
        p.setStattrak(item.stattrak());
        p.setPrecio(precio);
        p.setEstado(EstadoProducto.ACTIVO);
        p.setInventarioId(item.id());
        p.setDescripcion("Skin publicada desde el inventario del vendedor.");
        if (origen != null) {
            p.setFloatValor(origen.getFloatValor());
            origen.getImagenes().forEach(imagen -> agregarImagenDeInventario(p, imagen.getUrlImagen(), imagen.isEsPrincipal()));
        }
        if (p.getImagenes().isEmpty() && item.imagen() != null) {
            agregarImagenDeInventario(p, item.imagen(), true);
        }
        try {
            return productos.saveAndFlush(p);
        } catch (DataIntegrityViolationException e) {
            if (e.getCause() instanceof ConstraintViolationException conflicto
                    && conflicto.getKind() == ConstraintViolationException.ConstraintKind.UNIQUE) {
                throw new IllegalStateException("El ítem no está disponible para publicar.", e);
            }
            throw e;
        }
    }

    private void agregarImagenDeInventario(Producto producto, String url, boolean principal) {
        var imagen = new ImagenProducto();
        imagen.setProducto(producto);
        imagen.setUrlImagen(url);
        imagen.setEsPrincipal(principal);
        producto.getImagenes().add(imagen);
    }

    private Categoria categoriaPara(String tipo) {
        String categoriaNombre =
                switch (tipo) {
                    case "rifle" -> "Rifles";
                    case "francotirador" -> "Rifles de francotirador";
                    case "pistola" -> "Pistolas";
                    case "subfusil" -> "Subfusiles";
                    case "escopeta" -> "Escopetas";
                    case "ametralladora" -> "Ametralladoras";
                    case "cuchillo" -> "Cuchillos";
                    case "guantes" -> "Guantes";
                    default -> "Stickers";
                };
        return categorias.findByNombreIgnoreCase(categoriaNombre)
                .orElseThrow(() -> new IllegalStateException("La categoría de este ítem no está habilitada."));
    }
}
