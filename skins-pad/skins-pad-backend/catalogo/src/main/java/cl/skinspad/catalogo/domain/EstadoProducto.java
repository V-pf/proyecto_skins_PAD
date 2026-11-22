package cl.skinspad.catalogo.domain;

import java.util.Set;

// Estados de la publicación desde la revisión hasta la venta, el rechazo o el retiro.
public enum EstadoProducto {
    PENDIENTE_APROBACION, ACTIVO, INACTIVO, RECHAZADO, VENDIDO, ELIMINADO;

    public static final Set<EstadoProducto> RETIRADAS = Set.of(ELIMINADO, RECHAZADO);
}
