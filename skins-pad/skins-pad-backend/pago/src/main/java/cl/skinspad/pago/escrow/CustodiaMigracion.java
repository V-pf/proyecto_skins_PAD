package cl.skinspad.pago.escrow;

import cl.skinspad.pago.client.CatalogoPagoClient;
import cl.skinspad.pago.domain.EstadoPago;
import cl.skinspad.pago.repository.PagoRepository;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Importa el historial existente sin cobrar, abonar ni modificar el catálogo. */
@Service
@RequiredArgsConstructor
public class CustodiaMigracion {
    private final PagoRepository pagos;
    private final EventoRepository eventos;
    private final CatalogoPagoClient catalogo;

    @Value("${escrow.pago-horas:24}")
    private long pagoHoras = 24;

    @Transactional
    public void importar(Long pagoId) {
        var pago = pagos.bloquear(pagoId).orElseThrow();
        if (Boolean.TRUE.equals(pago.getCustodia())) return;
        var lineas = pago.getOperaciones().stream()
                .filter(o -> Operacion.PENDIENTE_IMPORTACION.equals(o.getCausa()))
                .sorted(java.util.Comparator.comparing(Operacion::getProductoId))
                .toList();
        if (lineas.isEmpty()) return;

        boolean completo = lineas.size() == pago.getOperaciones().size();
        long subtotal = 0;
        Instant fecha = pago.getFecha().atZone(ZoneId.of("America/Santiago")).toInstant();
        for (var o : lineas) {
            Long productoId = o.getProductoId();
            CatalogoPagoClient.Skin skin;
            try {
                skin = catalogo.buscar(productoId);
            } catch (feign.FeignException.NotFound ausente) {
                skin = null;
            }
            // Otros fallos HTTP abortan toda esta transacción: no se confunde
            // un servicio apagado con una publicación eliminada.
            boolean vendedorConocido = skin != null && skin.vendedorId() != null
                    && !skin.vendedorId().isBlank();
            o.setVendedorId(vendedorConocido ? skin.vendedorId() : "LEGACY_DESCONOCIDO");
            o.setNombre(skin != null && skin.nombre() != null ? skin.nombre() : "Skin #" + productoId);
            Long precio = entero(o.getPrecio());
            completo &= vendedorConocido && precio != null && precio > 0;
            if (precio != null && precio > 0) subtotal = Math.addExact(subtotal, precio);
        }

        Long total = entero(pago.getMontoTotal());
        completo &= total != null && total >= subtotal && subtotal > 0;
        // El porcentaje histórico no se conoce: no se inventa un 5% retrospectivo.
        // La comisión exacta sale de cobro menos precios guardados. -1 = sin tasa histórica.
        long acumulado = 0, asignado = 0;
        boolean activarPendiente = completo && pago.getEstado() == EstadoPago.PENDIENTE;
        for (var o : lineas) {
            o.setComisionBps(-1);
            if (completo) {
                acumulado = Math.addExact(acumulado, o.precioEnPesos());
                long bruto = BigInteger.valueOf(total).multiply(BigInteger.valueOf(acumulado))
                        .divide(BigInteger.valueOf(subtotal)).longValueExact();
                o.setComision(bruto - asignado - o.precioEnPesos());
                asignado = bruto;
            }
            if (activarPendiente) {
                o.setEstado(EstadoOperacion.PAGO_PENDIENTE);
                o.setHistorica(false);
                o.setCausa(null);
                o.setVence(Instant.now().plus(Duration.ofHours(pagoHoras)));
            } else if (!completo) {
                o.setEstado(EstadoOperacion.CONGELADA);
                o.setCausa("LEGACY_DATOS_INCOMPLETOS");
            } else {
                o.setEstado(switch (pago.getEstado()) {
                    case APROBADO -> Boolean.TRUE.equals(pago.getLiquidado())
                            ? EstadoOperacion.COMPLETADA : EstadoOperacion.CONGELADA;
                    case RECHAZADO -> EstadoOperacion.CANCELADA;
                    case REEMBOLSADO -> EstadoOperacion.REEMBOLSADA;
                    case PENDIENTE -> EstadoOperacion.CONGELADA;
                });
                o.setCausa("MIGRACION_HISTORICA_FECHA_APROXIMADA");
                if (o.getEstado() == EstadoOperacion.COMPLETADA) o.setCompletada(fecha);
            }
            eventos.save(new EventoOperacion(o, "MIGRACION", "IMPORTAR",
                    "Importación automática sin movimientos de dinero. "
                    + (completo ? "Se conserva el cobro y precio históricos."
                            : "Datos incompletos: requiere conciliación; no se estiman fondos.")));
        }
        if (activarPendiente) {
            // Reutiliza pedidoId: el job habitual recupera el débito idempotente,
            // sin volver a cobrar una referencia ya registrada por usuario.
            pago.setCustodia(true);
            pago.setEtapaCustodia("RESERVAR");
            pago.setLiquidado(false);
        }
    }

    private static Long entero(BigDecimal importe) {
        if (importe == null) return null;
        try { return importe.longValueExact(); }
        catch (ArithmeticException datoIncompatible) { return null; }
    }
}
