package cl.skinspad.pago.service;

import cl.skinspad.pago.client.UsuarioClient;
import cl.skinspad.pago.client.UsuarioRemoto;
import cl.skinspad.pago.domain.ComprobanteTransferencia;
import cl.skinspad.pago.domain.EstadoPago;
import cl.skinspad.pago.domain.MetodoPago;
import cl.skinspad.pago.domain.Pago;
import cl.skinspad.pago.dto.ComprobanteRequest;
import cl.skinspad.pago.dto.IniciarPagoRequest;
import cl.skinspad.pago.repository.ComprobanteRepository;
import cl.skinspad.pago.repository.PagoRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PagoService {

    private final PagoRepository pagos;
    private final ComprobanteRepository comprobantes;
    private final UsuarioClient usuarios;

    @Transactional
    public Pago iniciarPago(IniciarPagoRequest request) {
    MetodoPago.validarCompra(request.metodo());
        if (pagos.findByPedidoId(request.pedidoId()).isPresent()) {
            throw new IllegalArgumentException(
                "Ya existe un pago para el pedido " + request.pedidoId()
            );
        }

        // Todo pago debe estar relacionado con un usuario existente y no suspendido.
        UsuarioRemoto usuario = usuarios.buscar(request.usuarioId());
        if (Boolean.TRUE.equals(usuario.suspendido())) {
            throw new IllegalStateException("No se puede procesar un pago de una cuenta suspendida");
        }

        Pago pago = new Pago();
        pago.setPedidoId(request.pedidoId());
        pago.setUsuarioId(usuario.id());
        pago.setMontoTotal(request.montoTotal());
        pago.setMetodo(request.metodo());
        pago.setEstado(EstadoPago.PENDIENTE);
        pago.setReferenciaExterna(request.referenciaExterna());
        pago.setFecha(LocalDateTime.now());

        // El pago queda pendiente por defecto. Con saldo se aprueba después del débito.
        if (request.metodo() == MetodoPago.SALDO) {
            usuarios.debitar(
                usuario.id(),
                request.montoTotal(),
                "Pago del pedido " + request.pedidoId()
            );
            pago.setEstado(EstadoPago.APROBADO);
        }

        return pagos.save(pago);
    }

    public ComprobanteTransferencia comprobante(Long id){return comprobantes.findByPagoId(id).orElseThrow(()->new EntityNotFoundException("Comprobante no encontrado"));}

    @Transactional
    // Cada transferencia pendiente admite un comprobante, que queda por verificar.
    public ComprobanteTransferencia registrarComprobante(Long pagoId, ComprobanteRequest request) {
        Pago pago = buscar(pagoId);
        validarPagoPendienteDeTransferencia(pago);

        if (comprobantes.findByPagoId(pagoId).isPresent()) {
            throw new IllegalArgumentException("Este pago ya tiene un comprobante");
        }

        ComprobanteTransferencia comprobante = new ComprobanteTransferencia();
        comprobante.setPagoId(pagoId);
        comprobante.setBanco(request.banco());
        pago.setNumeroOperacion(request.numeroOperacion());
        comprobante.setNumeroOperacion(request.numeroOperacion());
        comprobante.setArchivoUrl(request.archivoUrl());
        comprobante.setVerificado(false);
        return comprobantes.save(comprobante);
    }

    @Transactional
    // La revisión del comprobante se guarda antes de confirmar el pago.
    public ComprobanteTransferencia verificarComprobante(Long pagoId, String urlVerificacion) {
        Pago pago = buscar(pagoId);
        validarPagoPendienteDeTransferencia(pago);

        ComprobanteTransferencia comprobante = comprobantes.findByPagoId(pagoId)
            .orElseThrow(() -> new EntityNotFoundException(
            "Comprobante no encontrado para pago " + pagoId
        ));
        comprobante.setVerificado(true);
        comprobante.setVerificadoUrl(urlVerificacion);
        return comprobantes.save(comprobante);
    }

    @Transactional
    // Una transferencia solo se aprueba cuando su comprobante está verificado.
    public Pago confirmarPago(Long pagoId) {
        Pago pago = buscar(pagoId);
        validarEstado(pago, EstadoPago.PENDIENTE, "Solo se puede confirmar un pago pendiente");

        if (pago.getMetodo() == MetodoPago.TRANSFERENCIA) {
            ComprobanteTransferencia comprobante = comprobantes.findByPagoId(pagoId)
                .orElseThrow(() -> new IllegalStateException(
                "Debe registrarse un comprobante antes de confirmar la transferencia"
            ));
            if (!comprobante.isVerificado()) {
                throw new IllegalStateException("El comprobante aún no ha sido verificado");
            }
        }

        pago.setEstado(EstadoPago.APROBADO);
        return pagos.save(pago);
    }

    @Transactional
    public Pago rechazar(Long pagoId, String motivo) {
        Pago pago = buscar(pagoId);
        validarEstado(pago, EstadoPago.PENDIENTE, "Solo se puede rechazar un pago pendiente");
        pago.setEstado(EstadoPago.RECHAZADO);
        pago.setReferenciaExterna(motivo);
        return pagos.save(pago);
    }

    @Transactional
    public Pago reembolsar(Long pagoId) {
        Pago pago = buscar(pagoId);
        validarEstado(pago, EstadoPago.APROBADO, "Solo se puede reembolsar un pago aprobado");

        // Se reembolsa el saldo en el microservicio de usuarios cuando corresponde.
        if (pago.getMetodo() == MetodoPago.SALDO) {
            usuarios.buscar(pago.getUsuarioId());
            usuarios.abonar(
                pago.getUsuarioId(),
                pago.getMontoTotal(),
                "Reembolso del pedido " + pago.getPedidoId()
            );
        }

        pago.setEstado(EstadoPago.REEMBOLSADO);
        return pagos.save(pago);
    }

    @Transactional(readOnly = true)
    public Pago buscar(Long id) {
        return pagos.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Pago no encontrado: " + id));
    }

    @Transactional(readOnly = true)
    public Pago buscarPorPedido(String pedidoId) {
        return pagos.findByPedidoId(pedidoId)
            .orElseThrow(() -> new EntityNotFoundException(
            "Pago no encontrado para pedido " + pedidoId
        ));
    }

    @Transactional(readOnly = true)
    public List<Pago> listarPorUsuario(String usuarioId) {
        return pagos.findAllByUsuarioIdOrderByFechaDesc(usuarioId);
    }

    private void validarPagoPendienteDeTransferencia(Pago pago) {
        if (pago.getMetodo() != MetodoPago.TRANSFERENCIA) {
            throw new IllegalStateException(
                "El comprobante solo se admite para pagos por transferencia"
            );
        }
        validarEstado(pago, EstadoPago.PENDIENTE, "El pago ya no está pendiente");
    }

    private void validarEstado(Pago pago, EstadoPago esperado, String mensaje) {
        if (pago.getEstado() != esperado) {
            throw new IllegalStateException(mensaje);
        }
    }
}
