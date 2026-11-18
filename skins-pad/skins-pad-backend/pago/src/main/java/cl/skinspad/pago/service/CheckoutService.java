package cl.skinspad.pago.service;

import cl.skinspad.pago.client.*;
import cl.skinspad.pago.client.CatalogoPagoClient.Reserva;
import cl.skinspad.pago.domain.*;
import cl.skinspad.pago.dto.CheckoutRequest;
import cl.skinspad.pago.repository.PagoRepository;
import java.math.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CheckoutService {
  private final PagoRepository pagos;
  private final PagoService servicio;
  private final UsuarioClient usuarios;
  private final CatalogoPagoClient catalogo;

  public synchronized Pago iniciar(String usuarioId, CheckoutRequest r) {
    // Si llega la misma solicitud otra vez, se retoma el pago que ya existe.
    var ref = "COMPRA-" + r.solicitudId();
    var previo = pagos.findByPedidoId(ref);
    if (previo.isPresent()) {
      if (!usuarioId.equals(previo.get().getUsuarioId()))
        throw new IllegalStateException("Referencia ajena");
      var existente = previo.get();
      if (existente.getEstado() == EstadoPago.APROBADO) stock(existente, "VENDER");
      else if (existente.getEstado() == EstadoPago.PENDIENTE
          && existente.getMetodo() == MetodoPago.SALDO) return confirmar(existente.getId());
      return existente;
    }
    if (new HashSet<>(r.productoIds()).size() != r.productoIds().size())
      throw new IllegalArgumentException("Solo una unidad por skin");
    var usuario = usuarios.buscar(usuarioId);
    if (Boolean.TRUE.equals(usuario.suspendido()))
      throw new IllegalStateException("Cuenta suspendida");
    // Con saldo, la skin se reserva de inmediato. Los demás medios parten con una cotización.
    boolean conSaldo = r.metodo() == MetodoPago.SALDO;
    var skins =
        catalogo.reservar(new Reserva(ref, r.productoIds(), conSaldo ? "RESERVAR" : "COTIZAR"));
    Pago pago = new Pago();
    try {
      if (skins.stream().anyMatch(s -> usuarioId.equals(s.vendedorId())))
        throw new IllegalStateException("No puedes comprar tu propia publicación");
      // El precio sale del catálogo. Se suma una comisión del 5 %, redondeada hacia abajo a pesos enteros.
      var subtotal =
          skins.stream()
              .map(CatalogoPagoClient.Skin::precio)
              .reduce(BigDecimal.ZERO, BigDecimal::add);
      var total =
          subtotal.add(subtotal.multiply(new BigDecimal("0.05")).setScale(0, RoundingMode.DOWN));
      if (r.metodo() == MetodoPago.SALDO && usuario.saldo().compareTo(total) < 0)
        throw new IllegalStateException("Saldo insuficiente");
      for (var skin : skins) pago.getPrecios().put(skin.id(), skin.precio());
      pago.setLiquidado(false);
      pago.setCuotas(1);
      pago.setUsuarioId(usuarioId);
      pago.setPedidoId(ref);
      pago.setProductoIds(new ArrayList<>(r.productoIds()));
      pago.setMetodo(r.metodo());
      pago.setMontoTotal(total);
      pago.setFecha(java.time.LocalDateTime.now());
      pago.setEstado(EstadoPago.PENDIENTE);
      // Se guarda el pago pendiente antes de intentar cobrar.
      pago = pagos.saveAndFlush(pago);
    } catch (RuntimeException e) {
      if (conSaldo) catalogo.reservar(new Reserva(ref, r.productoIds(), "LIBERAR"));
      throw e;
    }
    // Con saldo, se cobra antes de marcar las skins como vendidas.
    if (r.metodo() == MetodoPago.SALDO) {
      try {
        debitarCompra(pago);
        pago.setEstado(EstadoPago.APROBADO);
        pago = pagos.saveAndFlush(pago);
        stock(pago, "VENDER");
      } catch (IllegalStateException e) {
        throw e;
      } catch (RuntimeException e) {
        throw new IllegalStateException(
            "El pago #"
                + pago.getId()
                + " requiere revisión. Consulta Mis pagos antes de reintentar.");
      }
    }
    return pago;
  }

  // Si el saldo ya no alcanza, se rechaza el pago y se descarta la reserva.
  private void debitarCompra(Pago p) {
    try {
      usuarios.debitar(p.getUsuarioId(), p.getMontoTotal(), p.getPedidoId());
    } catch (feign.FeignException.Conflict rechazado) {
      servicio.rechazar(p.getId(), "El saldo disponible no permitió completar el débito.");
      stock(p, "DESCARTAR");
      throw new IllegalStateException("Pago rechazado por saldo insuficiente");
    }
  }

  // Al concretar la venta se abona a cada vendedor y se prepara el recibo del comprador.
  private void stock(Pago p, String accion) {
    if (p.getProductoIds().isEmpty()
        || ("VENDER".equals(accion) && Boolean.TRUE.equals(p.getLiquidado()))) return;
    var skins = catalogo.reservar(new Reserva(p.getPedidoId(), p.getProductoIds(), accion));
    if ("VENDER".equals(accion)) {
      StringBuilder detalle =
          new StringBuilder("Pago #")
              .append(p.getId())
              .append(" · ")
              .append(p.getFecha())
              .append("\n");
      for (var skin : skins) {
        usuarios.abonar(skin.vendedorId(), skin.precio(), "VENTA-" + p.getId() + "-" + skin.id());
        detalle
            .append("#")
            .append(skin.id())
            .append(" ")
            .append(skin.nombre())
            .append(" · CLP ")
            .append(skin.precio())
            .append("\n");
      }
      detalle
          .append("Total con comisión: CLP ")
          .append(p.getMontoTotal())
          .append(" · ")
          .append(p.getMetodo());
      usuarios.recibo(p.getUsuarioId(), "PAGO-" + p.getId(), detalle.toString());
      p.setLiquidado(true);
      pagos.saveAndFlush(p);
    }
  }

  public synchronized Pago confirmar(Long id) {
    var p = servicio.buscar(id);
    if (p.getEstado() != EstadoPago.APROBADO) {
      if (p.getEstado() != EstadoPago.PENDIENTE)
        throw new IllegalStateException("El pago ya está cerrado");
      if (p.getMetodo() == MetodoPago.SALDO && p.getEstado() == EstadoPago.PENDIENTE)
        debitarCompra(p);
      // Al confirmar por otro medio, se reservan las skins y se compara el precio con el cotizado.
      if (p.getMetodo() != MetodoPago.SALDO && !p.getProductoIds().isEmpty()) {
        var skins = catalogo.reservar(new Reserva(p.getPedidoId(), p.getProductoIds(), "RESERVAR"));
        try {
          for (var skin : skins) {
            var cotizado = p.getPrecios().get(skin.id());
            if (cotizado != null && cotizado.compareTo(skin.precio()) != 0)
              throw new IllegalStateException(
                  "El precio cambió. Rechaza el pago y solicita una nueva compra.");
          }
          p = servicio.confirmarPago(id);
        } catch (RuntimeException error) {
          stock(p, "DESCARTAR");
          throw error;
        }
      } else p = servicio.confirmarPago(id);
    }
    stock(p, "VENDER");
    return p;
  }

  // Un pago rechazado libera su reserva. Un débito de saldo pendiente requiere conciliación.
  public synchronized Pago rechazar(Long id, String motivo) {
    var p = servicio.buscar(id);
    if (p.getEstado() != EstadoPago.RECHAZADO) {
      if (p.getMetodo() == MetodoPago.SALDO)
        throw new IllegalStateException("Débito pendiente de conciliación");
      p = servicio.rechazar(id, motivo);
    }
    stock(p, "DESCARTAR");
    return p;
  }

  // Se revierten los abonos a los vendedores y se vuelven a dejar disponibles las skins.
  public synchronized Pago reembolsar(Long id) {
    var p = servicio.buscar(id);
    if (p.getEstado() != EstadoPago.REEMBOLSADO) {
      if (p.getEstado() != EstadoPago.APROBADO)
        throw new IllegalStateException("Solo se reembolsa un pago aprobado");
      for (Long productoId : p.getProductoIds()) {
        var skin = catalogo.buscar(productoId);
        usuarios.debitar(skin.vendedorId(), skin.precio(), "REVERSA-" + id + "-" + productoId);
      }
      p = servicio.reembolsar(id);
    }
    stock(p, "LIBERAR");
    return p;
  }

  public record Venta(
      Long id,
      java.time.LocalDateTime fecha,
      EstadoPago estado,
      List<Long> productoIds,
      Map<Long, BigDecimal> precios) {}

  // Cada vendedor recibe solo las skins y los precios que le corresponden en cada pago aprobado.
  public List<Venta> ventas(String id) {
    var propias =
        catalogo.porVendedor(id).stream()
            .map(CatalogoPagoClient.Skin::id)
            .collect(java.util.stream.Collectors.toSet());
    return pagos.findAll().stream()
        .filter(p -> p.getEstado() == EstadoPago.APROBADO)
        .map(
            p ->
                new Venta(
                    p.getId(),
                    p.getFecha(),
                    p.getEstado(),
                    p.getProductoIds().stream().filter(propias::contains).toList(),
                    p.getPrecios().entrySet().stream()
                        .filter(e -> propias.contains(e.getKey()))
                        .collect(
                            java.util.stream.Collectors.toMap(
                                Map.Entry::getKey, Map.Entry::getValue))))
        .filter(v -> !v.productoIds().isEmpty())
        .toList();
  }

  public List<Pago> listar() {
    return pagos.findAll();
  }
}
