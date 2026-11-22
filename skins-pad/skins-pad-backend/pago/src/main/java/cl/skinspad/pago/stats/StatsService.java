package cl.skinspad.pago.stats;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import cl.skinspad.pago.domain.MetodoPago;
import cl.skinspad.pago.repository.PagoRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StatsService {
  public static final ZoneId ZONE = ZoneId.of("America/Santiago");
  private final PagoRepository payments;
  private final StatsCatalogClient catalog;
  private final StatsUserClient users;
  private final cl.skinspad.pago.escrow.OperacionRepository operations;

  public StatsDto get(String auth, LocalDate from, LocalDate to, boolean compare, Long category, String method) {
    // Se valida el token también aquí: un encabezado X-Skins-Rol falsificado no concede acceso.
    if (auth == null || !auth.startsWith("Bearer ")) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    StatsUserClient.Identity identity;
    try { identity = users.validate(auth.substring(7)); }
    catch (feign.FeignException e) {
      throw new ResponseStatusException(e.status() >= 500 || e.status() < 0 ? HttpStatus.SERVICE_UNAVAILABLE : HttpStatus.UNAUTHORIZED);
    }
    if (!"admin".equals(identity.rol()) || identity.suspendido()) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    if (from != null && to != null && from.isAfter(to)) throw new IllegalArgumentException("El inicio debe ser anterior al fin.");
    if (method != null && !method.isBlank()) {
      try { MetodoPago.valueOf(method); } catch (Exception e) { throw new IllegalArgumentException("Método de pago inválido."); }
    } else method = null;
    if (category != null && category < 1) throw new IllegalArgumentException("Categoría inválida.");
    LocalDate effectiveTo = to == null ? LocalDate.now(ZONE) : to;
    if (effectiveTo.isAfter(LocalDate.now(ZONE))) throw new IllegalArgumentException("No selecciones fechas futuras.");
    if (from != null && from.isAfter(effectiveTo)) throw new IllegalArgumentException("Rango inválido.");
    LocalDate previousFrom = compare && from != null ? from.minusDays(ChronoUnit.DAYS.between(from,effectiveTo)+1) : null;
    var legacy = payments.findForStats(previousFrom != null ? previousFrom.atStartOfDay() : from == null ? null : from.atStartOfDay(), effectiveTo.plusDays(1).atStartOfDay());
    var errors = new LinkedHashMap<String,String>();
    List<StatsCatalogClient.Product> products = List.of();
    List<StatsCatalogClient.Category> categories = List.of();
    List<StatsUserClient.PublicUser> clients = List.of();
    try { products = catalog.products(); categories = catalog.categories(); }
    catch (feign.FeignException e) { errors.put("catalog", "No se pudo consultar el catálogo. Reintenta para recuperar categorías, rankings y publicaciones."); }
    try { clients = users.users(); }
    catch (feign.FeignException e) { errors.put("users", "No se pudieron consultar los clientes."); }
    var ps=new ArrayList<cl.skinspad.pago.domain.Pago>();
    legacy.stream().filter(p->!Boolean.TRUE.equals(p.getCustodia()) && (p.getEstado()!=cl.skinspad.pago.domain.EstadoPago.APROBADO || Boolean.TRUE.equals(p.getLiquidado()))).forEach(ps::add);
    var byId=payments.findAll().stream().collect(java.util.stream.Collectors.toMap(cl.skinspad.pago.domain.Pago::getId,p->p));
    var all=operations.findAll().stream().filter(o -> {
      var p = byId.get(o.getPagoId());
      return !o.isHistorica() && p != null && !Boolean.TRUE.equals(p.getSimulado());
    }).toList();
    for(var o:all){if(o.getEstado()!=cl.skinspad.pago.escrow.EstadoOperacion.COMPLETADA || o.getCompletada()==null)continue;
      var original=byId.get(o.getPagoId());if(original==null)continue;
      var p=new cl.skinspad.pago.domain.Pago();p.setId(original.getId());p.setUsuarioId(o.getCompradorId());p.setPedidoId(original.getPedidoId());p.setMetodo(original.getMetodo());p.setEstado(cl.skinspad.pago.domain.EstadoPago.APROBADO);p.setLiquidado(true);p.setFecha(o.getCompletada().atZone(ZONE).toLocalDateTime());p.setMontoTotal(java.math.BigDecimal.valueOf(o.getPrecio()+o.getComision()));p.setProductoIds(List.of(o.getProductoId()));p.setPrecios(Map.of(o.getProductoId(),java.math.BigDecimal.valueOf(o.getPrecio())));ps.add(p);
    }
    final var selectedMethod=method;final var selectedProducts=products;
    var selected=all.stream().filter(o->{var d=o.getCreada().atZone(ZONE).toLocalDate();var p=byId.get(o.getPagoId());return (from==null||!d.isBefore(from))&&!d.isAfter(effectiveTo)&&(selectedMethod==null||p!=null&&p.getMetodo().name().equals(selectedMethod))&&(category==null||selectedProducts.stream().anyMatch(x->x.id().equals(o.getProductoId())&&x.categoria()!=null&&category.equals(x.categoria().id())));}).toList();
    var custody=new ArrayList<StatsDto.Group>();
    for(var state:cl.skinspad.pago.escrow.EstadoOperacion.values()){var group=selected.stream().filter(o->o.getEstado()==state).toList();custody.add(new StatsDto.Group(state.name(),state.name(),group.size(),group.stream().mapToLong(o->o.getPrecio()+o.getComision()).sum()));}
    for(String cause:List.of("EXPIRADA","RECHAZADA_POR_VENDEDOR")){var group=selected.stream().filter(o->cause.equals(o.getCausa())).toList();custody.add(new StatsDto.Group(cause,cause,group.size(),group.stream().mapToLong(o->o.getPrecio()+o.getComision()).sum()));}
    var held=selected.stream().filter(cl.skinspad.pago.escrow.Operacion::isRetenido).toList();custody.add(new StatsDto.Group("EN_CUSTODIA","Total retenido",held.size(),held.stream().mapToLong(o->o.getPrecio()+o.getComision()).sum()));
    return StatsCalculator.calculate(ps,products,categories,clients,from,effectiveTo,compare,category,method,LocalDateTime.now(ZONE),errors).withCustody(custody);
  }
}
