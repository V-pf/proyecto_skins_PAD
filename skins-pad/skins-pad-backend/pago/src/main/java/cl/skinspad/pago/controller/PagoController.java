package cl.skinspad.pago.controller;

import cl.skinspad.pago.domain.*;
import cl.skinspad.pago.dto.*;
import cl.skinspad.pago.service.PagoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/pagos")
@RequiredArgsConstructor
public class PagoController {
    private final PagoService service;
    private final cl.skinspad.pago.escrow.CustodiaCheckout custodia;
    private final cl.skinspad.pago.service.CheckoutService checkout;
    private final cl.skinspad.pago.escrow.OperacionRepository operaciones;
    @GetMapping("/mis-ventas") public List<cl.skinspad.pago.service.CheckoutService.Venta> ventas(@RequestHeader("X-Skins-Usuario") String id){
        var resultado=new java.util.ArrayList<>(checkout.ventas(id));
        var grupos=operaciones.findByVendedorIdOrderByIdDesc(id).stream().filter(o->!o.isHistorica() && o.getEstado()==cl.skinspad.pago.escrow.EstadoOperacion.COMPLETADA).collect(java.util.stream.Collectors.groupingBy(cl.skinspad.pago.escrow.Operacion::getPagoId));
        grupos.forEach((pagoId,lineas)->resultado.add(new cl.skinspad.pago.service.CheckoutService.Venta(pagoId,lineas.stream().map(cl.skinspad.pago.escrow.Operacion::getCompletada).max(java.util.Comparator.naturalOrder()).orElseThrow().atZone(java.time.ZoneId.of("America/Santiago")).toLocalDateTime(),EstadoPago.APROBADO,lineas.stream().map(cl.skinspad.pago.escrow.Operacion::getProductoId).toList(),lineas.stream().collect(java.util.stream.Collectors.toMap(cl.skinspad.pago.escrow.Operacion::getProductoId,o->java.math.BigDecimal.valueOf(o.getPrecio()))))));
        return resultado;
    }
    @GetMapping public List<Pago> listar(){return checkout.listar();}
    // La compra toma el identificador del comprador que entrega el gateway.
    @PostMapping("/checkout") @ResponseStatus(HttpStatus.CREATED)
    public Pago checkout(@RequestHeader("X-Skins-Usuario") String id,@Valid @RequestBody CheckoutRequest r){return custodia.iniciar(id,r);}
    @GetMapping("/{id}/comprobante") public ComprobanteTransferencia ver(@PathVariable Long id){return service.comprobante(id);}

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Pago iniciar(@Valid @RequestBody IniciarPagoRequest r) {
        return service.iniciarPago(r);
    }

    @GetMapping("/{id}")
    public Pago porId(@PathVariable Long id) {
        return service.buscar(id);
    }

    @GetMapping("/pedido/{pedidoId}")
    public Pago porPedido(@PathVariable String pedidoId) {
        return service.buscarPorPedido(pedidoId);
    }

    @GetMapping("/pedido/{pedidoId}/estado")
    public EstadoPago estado(@PathVariable String pedidoId) {
        return service.buscarPorPedido(pedidoId).getEstado();
    }

    @GetMapping("/usuario/{usuarioId}")
    public List<Pago> usuario(@PathVariable String usuarioId) {
        return service.listarPorUsuario(usuarioId);
    }

    @PostMapping("/{id}/comprobante")
    @ResponseStatus(HttpStatus.CREATED)
    public ComprobanteTransferencia comprobante(@PathVariable Long id, @Valid @RequestBody ComprobanteRequest r) {
        return service.registrarComprobante(id, r);
    }

    @PostMapping("/{id}/comprobante/verificar")
    public ComprobanteTransferencia verificar(@PathVariable Long id, @RequestBody(required = false) VerificarComprobanteRequest r) {
        return service.verificarComprobante(id, r == null?null:r.verificadoUrl());
    }

    @PostMapping("/{id}/confirmar")
    public Pago confirmar(@PathVariable Long id) {
        return checkout.confirmar(id);
    }

    @PostMapping("/{id}/rechazar")
    public Pago rechazar(@PathVariable Long id, @Valid @RequestBody RechazoRequest r) {
        return checkout.rechazar(id, r.motivo());
    }

    @PostMapping("/{id}/reembolsar")
    public Pago reembolsar(@PathVariable Long id) {
        return checkout.reembolsar(id);
    }
}
