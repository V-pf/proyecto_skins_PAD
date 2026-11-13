package cl.skinspad.pago;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cl.skinspad.pago.client.UsuarioClient;
import cl.skinspad.pago.client.UsuarioRemoto;
import cl.skinspad.pago.domain.EstadoPago;
import cl.skinspad.pago.domain.MetodoPago;
import cl.skinspad.pago.domain.Pago;
import cl.skinspad.pago.dto.IniciarPagoRequest;
import cl.skinspad.pago.repository.ComprobanteRepository;
import cl.skinspad.pago.repository.PagoRepository;
import cl.skinspad.pago.service.PagoService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PagoServiceTest {

    @Mock
    private PagoRepository pagos;

    @Mock
    private ComprobanteRepository comprobantes;

    @Mock
    private UsuarioClient usuarios;

    @InjectMocks
    private PagoService service;

    @Test
    void iniciaPagoYVerificaLaExistenciaDelUsuario() {
        when(pagos.findByPedidoId("SP-1")).thenReturn(Optional.empty());
        when(usuarios.buscar("u1")).thenReturn(
            new UsuarioRemoto("u1", new BigDecimal("3000"), false)
        );
        when(pagos.save(any(Pago.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Pago pago = service.iniciarPago(new IniciarPagoRequest(
            "SP-1", "u1", new BigDecimal("1000"), MetodoPago.TARJETA, null
        ));

        assertEquals("u1", pago.getUsuarioId());
        assertEquals(EstadoPago.PENDIENTE, pago.getEstado());
        verify(usuarios).buscar("u1");
    }

    @Test
    void noPermitePagoDeUsuarioSuspendido() {
        when(pagos.findByPedidoId("SP-2")).thenReturn(Optional.empty());
        when(usuarios.buscar("u5")).thenReturn(
            new UsuarioRemoto("u5", BigDecimal.ZERO, true)
        );

        assertThrows(IllegalStateException.class, () -> service.iniciarPago(
            new IniciarPagoRequest("SP-2", "u5", new BigDecimal("1000"), MetodoPago.TARJETA, null)
        ));
        verify(pagos, never()).save(any(Pago.class));
    }

    @Test
    void pagoConSaldoDebitaAlUsuarioYQuedaAprobado() {
        when(pagos.findByPedidoId("SP-3")).thenReturn(Optional.empty());
        when(usuarios.buscar("u1")).thenReturn(
            new UsuarioRemoto("u1", new BigDecimal("3000"), false)
        );
        when(pagos.save(any(Pago.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Pago pago = service.iniciarPago(new IniciarPagoRequest(
            "SP-3", "u1", new BigDecimal("1000"), MetodoPago.SALDO, null
        ));

        assertEquals(EstadoPago.APROBADO, pago.getEstado());
        verify(usuarios).debitar("u1", new BigDecimal("1000"), "Pago del pedido SP-3");
    }

    @Test
    void confirmaPagoPendienteDesdeElServicio() {
        Pago pago = new Pago(
            1L, "SP-4", "u1", new BigDecimal("1000"), MetodoPago.TARJETA,
            EstadoPago.PENDIENTE, null, LocalDateTime.now()
        );
        when(pagos.findById(1L)).thenReturn(Optional.of(pago));
        when(pagos.save(pago)).thenReturn(pago);

        Pago resultado = service.confirmarPago(1L);

        assertEquals(EstadoPago.APROBADO, resultado.getEstado());
    }

    @Test
    void noPermiteReembolsarUnPagoNoAprobado() {
        Pago pago = new Pago(
            1L, "SP-5", "u1", new BigDecimal("1000"), MetodoPago.TARJETA,
            EstadoPago.PENDIENTE, null, LocalDateTime.now()
        );
        when(pagos.findById(1L)).thenReturn(Optional.of(pago));

        assertThrows(IllegalStateException.class, () -> service.reembolsar(1L));
    }
}
