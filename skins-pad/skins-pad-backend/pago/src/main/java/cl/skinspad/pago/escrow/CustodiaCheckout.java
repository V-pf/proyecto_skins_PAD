package cl.skinspad.pago.escrow;
import cl.skinspad.pago.client.*;
import cl.skinspad.pago.domain.*;
import cl.skinspad.pago.dto.CheckoutRequest;
import cl.skinspad.pago.repository.PagoRepository;
import java.math.*;import java.time.*;import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
@Service @RequiredArgsConstructor
public class CustodiaCheckout {
 private final PagoRepository pagos;private final OperacionRepository operaciones;private final EventoRepository eventos;
 private final UsuarioClient usuarios;private final CatalogoPagoClient catalogo;private final TransactionTemplate tx;private final CustodiaService custodia;
 @org.springframework.beans.factory.annotation.Value("${escrow.pago-horas:24}") private long pagoHoras=24;
 public Pago iniciar(String comprador,CheckoutRequest r){
   String ref="COMPRA-"+r.solicitudId();var previo=pagos.findByPedidoId(ref);
   if(previo.isPresent())return existente(previo.get(),comprador,r);
   if(r.productoIds().isEmpty() || new HashSet<>(r.productoIds()).size()!=r.productoIds().size() || r.productoIds().size()>50)throw new IllegalArgumentException("Selecciona entre 1 y 50 skins distintas");
   var u=usuarios.buscar(comprador);if(Boolean.TRUE.equals(u.suspendido()))throw new IllegalStateException("Cuenta suspendida");
   var skins=catalogo.reservar(new CatalogoPagoClient.Reserva(ref,r.productoIds(),"COTIZAR"));
   if(skins.size()!=r.productoIds().size())throw new IllegalStateException("Cotización incompleta");
   long subtotal=0;for(var skin:skins){if(comprador.equals(skin.vendedorId())||skin.vendedorId()==null)throw new IllegalStateException("Vendedor inválido o publicación propia");long precio=skin.precio().longValueExact();if(precio<=0)throw new IllegalArgumentException("Precio inválido");subtotal=Math.addExact(subtotal,precio);}
   final long base=subtotal,comision=BigInteger.valueOf(base).multiply(BigInteger.valueOf(500)).divide(BigInteger.valueOf(10000)).longValueExact(),total=Math.addExact(base,comision);
   if(r.metodo()==MetodoPago.SALDO && u.saldo().compareTo(BigDecimal.valueOf(total))<0)throw new IllegalStateException("Saldo insuficiente");
   Pago p;
   try {p=tx.execute(status->{
     var nuevo=new Pago();nuevo.setCustodia(true);nuevo.setEtapaCustodia("RESERVAR");nuevo.setLiquidado(false);nuevo.setPedidoId(ref);nuevo.setUsuarioId(comprador);nuevo.setMetodo(r.metodo());nuevo.setMontoTotal(BigDecimal.valueOf(total));nuevo.setCuotas(1);nuevo.setFecha(LocalDateTime.now(ZoneId.of("America/Santiago")));nuevo.setProductoIds(new ArrayList<>(r.productoIds()));
     skins.forEach(sk->nuevo.getPrecios().put(sk.id(),sk.precio()));pagos.saveAndFlush(nuevo);
     long acumulado=0,asignado=0;
     for(var skin:skins.stream().sorted(Comparator.comparing(CatalogoPagoClient.Skin::id)).toList()){
       var o=new Operacion();o.setPagoId(nuevo.getId());o.setProductoId(skin.id());o.setCompradorId(comprador);o.setVendedorId(skin.vendedorId());o.setNombre(skin.nombre()==null?"Skin #"+skin.id():skin.nombre());o.setPrecio(skin.precio().longValueExact());
       acumulado+=o.getPrecio();long cuota=BigInteger.valueOf(acumulado).multiply(BigInteger.valueOf(comision)).divide(BigInteger.valueOf(base)).longValueExact();o.setComision(cuota-asignado);asignado=cuota;
       o.setVence(Instant.now().plus(Duration.ofHours(pagoHoras)));operaciones.saveAndFlush(o);eventos.save(new EventoOperacion(o,comprador,"CREAR","Compra con custodia; entrega simulada"));
     }return nuevo;
   });}catch(org.springframework.dao.DataIntegrityViolationException e){return existente(pagos.findByPedidoId(ref).orElseThrow(()->e),comprador,r);}
   custodia.pago(p.getId());return pagos.findById(p.getId()).orElseThrow();
 }
 private Pago existente(Pago p,String comprador,CheckoutRequest r){
   if(!comprador.equals(p.getUsuarioId()) || p.getMetodo()!=r.metodo() || !new HashSet<>(p.getProductoIds()).equals(new HashSet<>(r.productoIds())))throw new IllegalStateException("Referencia utilizada para otra solicitud");
   if(Boolean.TRUE.equals(p.getCustodia()))custodia.pago(p.getId());return pagos.findById(p.getId()).orElse(p);
 }
}
