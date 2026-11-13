package cl.skinspad.pago;
import cl.skinspad.pago.client.*;
import cl.skinspad.pago.domain.*;
import cl.skinspad.pago.dto.CheckoutRequest;
import cl.skinspad.pago.repository.PagoRepository;
import cl.skinspad.pago.service.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class CheckoutServiceTest {
    final PagoRepository pagos=mock(PagoRepository.class);
    final UsuarioClient usuarios=mock(UsuarioClient.class);
    final CatalogoPagoClient catalogo=mock(CatalogoPagoClient.class);
    final CheckoutService checkout=new CheckoutService(pagos,mock(PagoService.class),usuarios,catalogo);
    CheckoutRequest solicitud(MetodoPago metodo){return new CheckoutRequest("prueba",List.of(1L),metodo);}
    void preparar(String saldo,String vendedor){
        when(usuarios.buscar("u1")).thenReturn(new UsuarioRemoto("u1",new BigDecimal(saldo),false));
        when(catalogo.reservar(any())).thenReturn(List.of(new CatalogoPagoClient.Skin(1L,vendedor,new BigDecimal("190"))));
    }
    @Test void totalProvieneDelCatalogo(){
        preparar("1000","u3");
        when(pagos.saveAndFlush(any())).thenAnswer(x->x.getArgument(0));
        var pago=checkout.iniciar("u1",solicitud(MetodoPago.TARJETA));
        assertEquals(199,pago.getMontoTotal().intValueExact());assertEquals(EstadoPago.PENDIENTE,pago.getEstado());
    }
    @Test void liberaSiSaldoNoAlcanza(){
        preparar("0","u3");assertThrows(IllegalStateException.class,()->checkout.iniciar("u1",solicitud(MetodoPago.SALDO)));
        verify(catalogo).reservar(argThat(r->r.accion().equals("LIBERAR")));verify(usuarios,never()).debitar(any(),any(),any());
    }
    @Test void reintentoDevuelveMismoPago(){
        Pago p=new Pago();p.setUsuarioId("u1");when(pagos.findByPedidoId("COMPRA-prueba")).thenReturn(Optional.of(p));
        assertSame(p,checkout.iniciar("u1",solicitud(MetodoPago.SALDO)));verifyNoInteractions(usuarios,catalogo);
    }
    @Test void noPermiteComprarPublicacionPropia(){
        preparar("1000","u1");assertThrows(IllegalStateException.class,()->checkout.iniciar("u1",solicitud(MetodoPago.TARJETA)));
        verify(pagos,never()).saveAndFlush(any());
    }

    @Test void tarjetaSoloCotizaYConservaPrecio() {
        preparar("0","u3");
        when(pagos.saveAndFlush(any())).thenAnswer(x->x.getArgument(0));
        var pago=checkout.iniciar("u1",new CheckoutRequest("prueba",List.of(1L),MetodoPago.TARJETA));
        verify(catalogo).reservar(new CatalogoPagoClient.Reserva("COMPRA-prueba",List.of(1L),"COTIZAR"));
        assertEquals(new BigDecimal("190"),pago.getPrecios().get(1L));
        assertEquals(1,pago.getCuotas());
    }
    @Test void saldoAcreditaAlVendedorSinComision() {
        preparar("1000","u3");
        when(pagos.saveAndFlush(any())).thenAnswer(x->{Pago p=x.getArgument(0);p.setId(5L);return p;});
        var pago=checkout.iniciar("u1",solicitud(MetodoPago.SALDO));
        verify(usuarios).debitar("u1",new BigDecimal("199"),"COMPRA-prueba");
        verify(usuarios).abonar("u3",new BigDecimal("190"),"VENTA-5-1");
        assertTrue(pago.getLiquidado());
    }
}
