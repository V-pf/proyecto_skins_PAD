package cl.skinspad.pago.escrow;
import cl.skinspad.pago.client.*;
import cl.skinspad.pago.domain.*;
import cl.skinspad.pago.repository.PagoRepository;
import java.math.*;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import static cl.skinspad.pago.escrow.MaquinaOperacion.Accion.*;
@Service @RequiredArgsConstructor
public class CustodiaTransactions {
 private final OperacionRepository operaciones;
 private final PagoRepository pagos;
 private final EventoRepository eventos;
 private final AsientoRepository asientos;
 private final CatalogoPagoClient catalogo;
 private final UsuarioClient usuarios;
 @Value("${escrow.aceptacion-horas:24}") private long aceptarHoras=24;
 @Value("${escrow.entrega-horas:48}") private long entregaHoras=48;
 @Value("${escrow.recepcion-horas:72}") private long recibirHoras=72;
 private Pago pago(Long id){return pagos.bloquear(id).orElseThrow();}
 private Operacion operacion(Long id){return operaciones.bloquear(id).orElseThrow();}
 @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED) public void procesarPago(Long id){
   var p=pago(id);String etapa=p.getEtapaCustodia();
   if(etapa==null || etapa.equals("LISTO"))return;
   var os=operaciones.findByPagoIdOrderById(id);
   if(etapa.equals("CANCELAR")){
     catalogo.reservar(new CatalogoPagoClient.Reserva(p.getPedidoId(),p.getProductoIds(),"DESCARTAR"));
     for(var o:os){o.setEstado(MaquinaOperacion.siguiente(o.getEstado(),CANCELAR));o.setCausa("RESERVA_O_SALDO_RECHAZADO");o.setVence(null);eventos.save(new EventoOperacion(o,"SISTEMA","CANCELAR",p.getErrorCustodia()));}
     p.setEstado(EstadoPago.RECHAZADO);p.setEtapaCustodia("LISTO");p.setErrorCustodia(null);return;
   }
   // Precio y vendedor se congelaron al crear la orden; se contrastan al reservar.
   var skins=catalogo.reservar(new CatalogoPagoClient.Reserva(p.getPedidoId(),p.getProductoIds(),"RESERVAR"));
   boolean simulado = Boolean.TRUE.equals(p.getSimulado());
   for(var o:os){var skin=skins.stream().filter(x->x.id().equals(o.getProductoId())).findFirst().orElseThrow();
     if(!o.getVendedorId().equals(skin.vendedorId()) || o.precioEnPesos()!=skin.precio().longValueExact())throw new IllegalArgumentException("La cotización cambió");}
   // COBRAR permite recuperar los pagos que ya se habían verificado antes de este cambio.
   if(simulado || etapa.equals("COBRAR") || p.getMetodo()==MetodoPago.SALDO){
     if(p.getMetodo()==MetodoPago.SALDO)usuarios.debitar(p.getUsuarioId(),p.getMontoTotal(),p.getPedidoId());
     Instant ahora = Instant.now();
     for (var o : os) {
       o.setEstado(MaquinaOperacion.siguiente(o.getEstado(), VERIFICAR));
       o.setRetenido(!simulado);
       o.setVence(ahora.plus(Duration.ofHours(aceptarHoras)));
       o.setActualizada(ahora);
       if (!simulado) {
         asientos.save(new AsientoCustodia(o, "RETENCION", Math.addExact(o.precioEnPesos(), o.getComision()), p.getUsuarioId()));
       }
       eventos.save(new EventoOperacion(o, "SISTEMA", "VERIFICAR",
           simulado ? "Pago simulado. Esperando al vendedor." : "Fondos retenidos. Esperando al vendedor."));
     }
     p.setEstado(EstadoPago.APROBADO);
   }
   p.setEtapaCustodia("LISTO");p.setErrorCustodia(null);
 }
 @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED) public void falloPago(Long id,boolean definitivo){var p=pago(id);p.setErrorCustodia(definitivo?"Reserva o débito rechazado; cancelación en curso":"Comunicación pendiente; recuperación automática");if(definitivo)p.setEtapaCustodia("CANCELAR");}
 @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED) public void solicitar(Long id,MaquinaOperacion.Accion a,String actor,String rol,String motivo,boolean sistema){
   var p=pago(operaciones.pagoId(id));var o=operacion(id);
   if(!"LISTO".equals(p.getEtapaCustodia()))throw new IllegalStateException("Pago en conciliación");
   if(o.isHistorica())throw new IllegalStateException("Operación histórica de solo lectura");
   boolean admin="admin".equals(rol);
   boolean autorizado=switch(a){
     case ACEPTAR,RECHAZAR,ENTREGAR -> !admin && o.getVendedorId().equals(actor);
     case RECIBIR,DISPUTAR -> !admin && o.getCompradorId().equals(actor);
     case CONGELAR,RESOLVER_COMPRADOR,RESOLVER_VENDEDOR -> admin;
     case CANCELAR -> !admin && o.getCompradorId().equals(actor);
     case AUTO_LIBERAR,EXPIRAR -> sistema;
     default -> false;
   };
   if(!autorizado)throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN);
   if((admin || a==DISPUTAR || a==RECHAZAR) && (motivo==null || motivo.isBlank()))throw new IllegalArgumentException("Motivo obligatorio");
   if(motivo!=null && motivo.length()>1000)throw new IllegalArgumentException("Motivo demasiado largo");
   if(o.getAccionPendiente()!=null){if(o.getAccionPendiente().equals(a.name()) && actor.equals(o.getActorPendiente()))return;throw new IllegalStateException("Acción en recuperación; no se puede iniciar otra");}
   if(eventos.findByOperacionIdOrderById(id).stream().anyMatch(e->e.getAccion().equals(a.name()) && e.getActor().equals(actor)))return;
   if(sistema && (o.getVence()==null || o.getVence().isAfter(Instant.now())))throw new IllegalStateException("El plazo no venció");
   if(!sistema && (a==ACEPTAR || a==ENTREGAR) && o.getVence()!=null && !o.getVence().isAfter(Instant.now()))throw new IllegalStateException("Plazo vencido");
   MaquinaOperacion.siguiente(o.getEstado(), a, o.isRetenido() || Boolean.TRUE.equals(p.getSimulado()));
   o.setAccionPendiente(a.name());o.setActorPendiente(actor);o.setMotivoPendiente(motivo);o.setSincronizado(false);
 }
 @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED) public void procesar(Long id){
   var p=pago(operaciones.pagoId(id));var o=operacion(id);
   if(o.getAccionPendiente()==null)return;
   var a=MaquinaOperacion.Accion.valueOf(o.getAccionPendiente());
   var siguiente=MaquinaOperacion.siguiente(o.getEstado(),a,o.isRetenido() || Boolean.TRUE.equals(p.getSimulado()));
   String actor=o.getActorPendiente();
   if(siguiente==EstadoOperacion.COMPLETADA){
     catalogo.reservar(new CatalogoPagoClient.Reserva(p.getPedidoId(),List.of(o.getProductoId()),"VENDER"));
     if (o.isRetenido()) {
       usuarios.abonar(o.getVendedorId(), BigDecimal.valueOf(o.precioEnPesos()), "ESCROW-LIBERAR-" + o.getId());
       asientos.save(new AsientoCustodia(o, "LIBERACION", o.precioEnPesos(), actor));
       asientos.save(new AsientoCustodia(o, "COMISION", o.getComision(), actor));
       o.setRetenido(false);
     }
     o.setCompletada(Instant.now());
   } else if(siguiente==EstadoOperacion.REEMBOLSADA || siguiente==EstadoOperacion.CANCELADA){
     if(o.isRetenido()){usuarios.abonar(o.getCompradorId(),BigDecimal.valueOf(Math.addExact(o.precioEnPesos(),o.getComision())),"ESCROW-REEMBOLSO-"+o.getId());asientos.save(new AsientoCustodia(o,"REEMBOLSO",Math.addExact(o.precioEnPesos(),o.getComision()),actor));o.setRetenido(false);}
     // DESCARTAR no toca una reserva que ya pertenezca a otra operación.
     catalogo.reservar(new CatalogoPagoClient.Reserva(p.getPedidoId(),List.of(o.getProductoId()),"DESCARTAR"));
     if(a==RECHAZAR)o.setCausa("RECHAZADA_POR_VENDEDOR");else if(a==EXPIRAR)o.setCausa("EXPIRADA");else o.setCausa(a.name());
   }
   o.setEstado(siguiente);o.setActualizada(Instant.now());
   o.setVence(siguiente==EstadoOperacion.ACEPTADA_POR_VENDEDOR?Instant.now().plus(Duration.ofHours(entregaHoras)):siguiente==EstadoOperacion.ENTREGADA?Instant.now().plus(Duration.ofHours(recibirHoras)):null);
   eventos.save(new EventoOperacion(o,actor,a.name(),o.getMotivoPendiente()));o.setAccionPendiente(null);o.setActorPendiente(null);o.setMotivoPendiente(null);o.setSincronizado(true);o.setErrorSincronizacion(null);
   var os=operaciones.findByPagoIdOrderById(p.getId());
   p.setLiquidado(os.stream().allMatch(x->MaquinaOperacion.terminal(x.getEstado())));
   if(os.stream().allMatch(x->x.getEstado()==EstadoOperacion.REEMBOLSADA))p.setEstado(EstadoPago.REEMBOLSADO);
   else if(os.stream().allMatch(x->x.getEstado()==EstadoOperacion.CANCELADA))p.setEstado(EstadoPago.RECHAZADO);
 }
 @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED) public void fallo(Long id){pago(operaciones.pagoId(id));var o=operacion(id);o.setErrorSincronizacion("Sincronización pendiente; se reintentará sin duplicar movimientos");}
}
