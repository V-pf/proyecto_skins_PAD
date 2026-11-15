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
    var ps = payments.findForStats(previousFrom != null ? previousFrom.atStartOfDay() : from == null ? null : from.atStartOfDay(), effectiveTo.plusDays(1).atStartOfDay());
    var errors = new LinkedHashMap<String,String>();
    List<StatsCatalogClient.Product> products = List.of();
    List<StatsCatalogClient.Category> categories = List.of();
    List<StatsUserClient.PublicUser> clients = List.of();
    try { products = catalog.products(); categories = catalog.categories(); }
    catch (feign.FeignException e) { errors.put("catalog", "No se pudo consultar el catálogo. Reintenta para recuperar categorías, rankings y publicaciones."); }
    try { clients = users.users(); }
    catch (feign.FeignException e) { errors.put("users", "No se pudieron consultar los clientes."); }
    return StatsCalculator.calculate(ps,products,categories,clients,from,effectiveTo,compare,category,method,LocalDateTime.now(ZONE),errors);
  }
}
