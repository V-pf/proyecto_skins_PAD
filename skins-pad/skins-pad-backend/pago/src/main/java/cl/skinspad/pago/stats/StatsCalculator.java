package cl.skinspad.pago.stats;

import cl.skinspad.pago.domain.*;
import java.math.*;
import java.text.Normalizer;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import static cl.skinspad.pago.stats.StatsDto.*;
import static cl.skinspad.pago.stats.StatsCatalogClient.*;

/** Todas las vistas se derivan de las mismas líneas de pagos APROBADO. */
public final class StatsCalculator {
  private StatsCalculator() {}
  private record Line(Pago payment, Long productId, Product product, Long net, Long gross) {}
  private record Order(Pago payment, List<Line> lines, Long gross) {}
  private static long clp(BigDecimal n) { return n.setScale(0,RoundingMode.HALF_UP).longValueExact(); }
  private static boolean in(LocalDateTime date, LocalDate from, LocalDate to) {
    return date != null && (from == null || !date.toLocalDate().isBefore(from)) && !date.toLocalDate().isAfter(to);
  }
  private static String category(Line l) { return l.product != null && l.product.categoria()!=null ? l.product.categoria().nombre() : "Sin categoría disponible"; }
  private static String categoryKey(Line l) { return l.product != null && l.product.categoria()!=null ? String.valueOf(l.product.categoria().id()) : "unknown"; }
  private static String name(Line l) { return l.product == null ? "Publicación #"+l.productId : l.product.nombre(); }
  private static String seller(Line l) { return l.product == null || l.product.vendedorId()==null ? "Sin vendedor disponible" : l.product.vendedorId(); }
  private static String image(Line l) {
    if(l.product==null) return "/img/logo.svg";
    String type=l.product.tipo();
    String fallback=type!=null && Set.of("rifle","pistola","francotirador","subfusil","cuchillo","guantes","sticker").contains(type)?"/img/arma-"+type+".svg":"/img/logo.svg";
    if(l.product.imagenes()==null) return fallback;
    return l.product.imagenes().stream().sorted(Comparator.comparing(Picture::esPrincipal).reversed()).map(Picture::urlImagen).findFirst().orElse(fallback);
  }
  private static Long sum(List<Long> values) { return values.stream().anyMatch(Objects::isNull) ? null : values.stream().mapToLong(Long::longValue).sum(); }
  private static Long net(List<Line> ls) { return sum(ls.stream().map(Line::net).toList()); }
  private static boolean test(Pago p, Map<Long,Product> products) {
    return (p.getPedidoId()!=null && (p.getPedidoId().startsWith("COMPRA-XAMPP-DEMO-") || p.getPedidoId().startsWith("COMPRA-demo-v1-")))
      || p.getProductoIds().stream().map(products::get).filter(Objects::nonNull).anyMatch(StatsCalculator::testProduct);
  }
  private static boolean testProduct(Product p) { return p.nombre()!=null && (p.nombre().startsWith("[XAMPP]") || p.nombre().startsWith("[DEMO]")); }

  /** Distribución entera por mayor resto: las partes siempre suman el cobro original. */
  static Map<Long,Long> allocate(Pago p) {
    var precios=p.getPrecios();
    var ids=p.getProductoIds().stream().distinct().sorted().toList();
    if(ids.isEmpty() || ids.stream().anyMatch(id->precios.get(id)==null)) return Map.of();
    BigDecimal total=ids.stream().map(precios::get).reduce(BigDecimal.ZERO,BigDecimal::add);
    if(total.signum()<=0 || ids.stream().anyMatch(id->precios.get(id).signum()<0)) return Map.of();
    long gross=clp(p.getMontoTotal());
    var result=new LinkedHashMap<Long,Long>(); var remainders=new HashMap<Long,BigDecimal>();
    for(Long id:ids) {
      BigDecimal weighted=precios.get(id).multiply(BigDecimal.valueOf(gross));
      long floor=weighted.divide(total,0,RoundingMode.DOWN).longValueExact();
      result.put(id,floor); remainders.put(id,weighted.subtract(total.multiply(BigDecimal.valueOf(floor))));
    }
    long remaining=gross-result.values().stream().mapToLong(Long::longValue).sum();
    var priority=ids.stream().sorted(Comparator.<Long,BigDecimal>comparing(remainders::get).reversed().thenComparingLong(Long::longValue)).toList();
    for(int i=0;i<remaining;i++)result.merge(priority.get(i%priority.size()),1L,Long::sum);
    return result;
  }
  private static List<Order> orders(List<Pago> ps, Map<Long,Product> products, LocalDate from, LocalDate to, Long cat, String method) {
    var result=new ArrayList<Order>();
    for(Pago p:ps) {
      if(!in(p.getFecha(),from,to) || test(p,products) || (method!=null && !method.equals(p.getMetodo().name())))continue;
      var allocations=allocate(p);
      var lines=p.getOperaciones().stream().map(o->new Line(p,o.getProductoId(),products.get(o.getProductoId()),
          o.getPrecio()==null?null:clp(o.getPrecio()),allocations.get(o.getProductoId())))
        .filter(l->cat==null || l.product!=null && l.product.categoria()!=null && cat.equals(l.product.categoria().id())).toList();
      if(cat!=null && lines.isEmpty())continue;
      result.add(new Order(p,lines,cat==null?clp(p.getMontoTotal()):sum(lines.stream().map(Line::gross).toList())));
    }
    return result;
  }
  private static List<Line> sold(List<Order> os) { return os.stream().filter(o->o.payment.getEstado()==EstadoPago.APROBADO).flatMap(o->o.lines.stream()).toList(); }
  private static List<Product> cohort(List<Product> ps, LocalDate from, LocalDate to, Long cat) {
    return ps.stream().filter(p->!testProduct(p) && in(p.fechaCreacion(),from,to) && (cat==null || p.categoria()!=null && cat.equals(p.categoria().id()))).toList();
  }
  private static Summary summary(List<Order> os, List<Product> cohort, boolean conversionAvailable) {
    var approved=os.stream().filter(o->o.payment.getEstado()==EstadoPago.APROBADO).toList();
    var lines=sold(os); long count=approved.stream().map(o->o.payment.getId()).distinct().count();
    Long gross=sum(approved.stream().map(Order::gross).toList());
    Long net=approved.stream().anyMatch(o->o.lines.isEmpty())?null:net(lines);
    Long commission=gross==null || net==null || gross<net?null:gross-net;
    var ids=lines.stream().map(Line::productId).collect(Collectors.toSet());
    Double conversion=!conversionAvailable || cohort.isEmpty()?null:100.0*cohort.stream().filter(p->ids.contains(p.id())).count()/cohort.size();
    return new Summary(gross,net,commission,count,gross==null || count==0?null:Math.round((double)gross/count),lines.size(),conversion,
      commission==null || net==null || net==0?null:100.0*commission/net,cohort.size());
  }
  private static List<Group> groupLines(List<Line> lines,Function<Line,String> key,Function<Line,String> label) {
    return lines.stream().collect(Collectors.groupingBy(key)).entrySet().stream().map(e->new Group(e.getKey(),label.apply(e.getValue().get(0)),e.getValue().size(),net(e.getValue())))
      .sorted(Comparator.comparingLong(Group::count).reversed().thenComparing(Group::label)).toList();
  }
  private static List<Group> groupProducts(List<Product> products,Function<Product,String> key) {
    return products.stream().collect(Collectors.groupingBy(p->Objects.toString(key.apply(p),"Sin dato"),Collectors.counting())).entrySet().stream()
      .map(e->new Group(e.getKey(),e.getKey(),e.getValue(),null)).sorted(Comparator.comparingLong(Group::count).reversed()).toList();
  }
  private static List<Rank> ranking(List<Line> lines,boolean sellers) {
    return lines.stream().collect(Collectors.groupingBy(l->sellers?seller(l):Normalizer.normalize(name(l),Normalizer.Form.NFC).trim().replaceAll("\\s+"," ").toLowerCase(Locale.ROOT)))
      .entrySet().stream().map(e->new Rank(e.getKey(),sellers?"Vendedor "+e.getKey():name(e.getValue().get(0)),sellers?null:image(e.getValue().get(0)),e.getValue().size(),net(e.getValue()),"APROBADO"))
      .sorted(sellers ? Comparator.comparing(Rank::income,Comparator.nullsLast(Comparator.reverseOrder())).thenComparing(Rank::key)
        : Comparator.comparingLong(Rank::units).reversed().thenComparing(Rank::income,Comparator.nullsLast(Comparator.reverseOrder())).thenComparing(Rank::key)).limit(5).toList();
  }
  public static StatsDto calculate(List<Pago> ps,List<Product> productList,List<Category> categories,List<StatsUserClient.PublicUser> clients,
      LocalDate from,LocalDate to,boolean compare,Long cat,String method,LocalDateTime now,Map<String,String> errors) {
    var products=productList.stream().collect(Collectors.toMap(Product::id,Function.identity()));
    var os=orders(ps,products,from,to,cat,method); var lines=sold(os); var created=cohort(productList,from,to,cat);
    var warnings=new ArrayList<String>();
    warnings.add("Ventas nuevas: fecha de finalización de custodia. Históricas: fecha de creación del pago, sin fecha verificable de entrega. Días civiles America/Santiago.");
    if(ps.stream().anyMatch(p->test(p,products)))warnings.add("Se excluyeron operaciones de prueba identificadas por [XAMPP], [DEMO] o referencia de carga.");
    if(lines.stream().anyMatch(l->l.net==null) || os.stream().anyMatch(o->o.payment.getEstado()==EstadoPago.APROBADO && o.lines.isEmpty()))warnings.add("Hay pagos sin desglose histórico completo: no se estima comisión ni se sustituye por el precio actual.");
    if(lines.stream().anyMatch(l->l.product==null))warnings.add("Hay referencias a publicaciones no disponibles. Se conservan sus unidades e importes históricos bajo Sin categoría disponible.");
    Map<String,String> expected=Map.of("rifle","Rifles","pistola","Pistolas","francotirador","Rifles de francotirador","subfusil","Subfusiles","cuchillo","Cuchillos","guantes","Guantes","sticker","Stickers");
    long mismatch=productList.stream().filter(p->!testProduct(p)&&p.categoria()!=null&&p.tipo()!=null&&expected.containsKey(p.tipo())&&!expected.get(p.tipo()).equals(p.categoria().nombre())).count();
    if(mismatch>0)warnings.add(mismatch+" publicaciones tienen tipo y categoría discordantes. Todos los gráficos usan la categoría asignada, sin reclasificar datos automáticamente.");
    boolean available=method==null && !errors.containsKey("catalog");
    var current=summary(os,created,available);
    if(current.gross()!=null && current.net()!=null && current.gross()<current.net())warnings.add("El cobro aprobado es menor que los precios históricos: comisión bloqueada hasta conciliar esa diferencia.");
    LocalDate prevTo=from==null?null:from.minusDays(1),prevFrom=from==null?null:from.minusDays(ChronoUnit.DAYS.between(from,to)+1);
    Summary previous=compare&&from!=null?summary(orders(ps,products,prevFrom,prevTo,cat,method),cohort(productList,prevFrom,prevTo,cat),available):null;
    if(compare&&from==null)warnings.add("Todo el historial no tiene período anterior equivalente.");
    if(method!=null)warnings.add("La conversión no está disponible al filtrar por método: las publicaciones sin compra no tienen método de pago.");
    var approved=os.stream().filter(o->o.payment.getEstado()==EstadoPago.APROBADO).toList();
    LocalDate start=from!=null?from:approved.stream().map(o->o.payment.getFecha().toLocalDate()).min(LocalDate::compareTo).orElse(to);
    boolean daily=ChronoUnit.DAYS.between(start,to)<=62;
    var timeline=new ArrayList<Bucket>();
    for(LocalDate d=daily?start:start.withDayOfMonth(1);!d.isAfter(to);d=daily?d.plusDays(1):d.plusMonths(1)) {
      final LocalDate bucket=d;
      var found=approved.stream().filter(o->daily?o.payment.getFecha().toLocalDate().equals(bucket):YearMonth.from(o.payment.getFecha()).equals(YearMonth.from(bucket))).toList();
      timeline.add(new Bucket(d.toString(),sum(found.stream().map(Order::gross).toList()),found.stream().map(o->o.payment.getId()).distinct().count()));
    }
    var funnel=new ArrayList<Group>();
    for(var state:EstadoPago.values()) { var group=os.stream().filter(o->o.payment.getEstado()==state).toList();funnel.add(new Group(state.name(),state.name(),group.size(),sum(group.stream().map(Order::gross).toList()))); }
    var methods=approved.stream().collect(Collectors.groupingBy(o->o.payment.getMetodo().name())).entrySet().stream().map(e->new Group(e.getKey(),e.getKey(),e.getValue().stream().map(o->o.payment.getId()).distinct().count(),sum(e.getValue().stream().map(Order::gross).toList())))
      .sorted(Comparator.comparing(Group::amount,Comparator.nullsLast(Comparator.reverseOrder()))).toList();
    var floatValues=lines.stream().filter(l->l.product!=null&&l.product.floatValor()!=null).mapToDouble(l->l.product.floatValor().doubleValue()).toArray();
    var prices=lines.stream().map(Line::net).filter(Objects::nonNull).sorted().toList(); int n=prices.size();
    var attrs=new Attributes(floatValues.length==0?null:Arrays.stream(floatValues).min().orElseThrow(),floatValues.length==0?null:Arrays.stream(floatValues).max().orElseThrow(),floatValues.length==0?null:Arrays.stream(floatValues).average().orElseThrow(),
      n==0?null:prices.get(0),n==0?null:prices.get(n-1),n==0?null:Math.round(((double)prices.get((n-1)/2)+prices.get(n/2))/2));
    var clientIds=clients.stream().filter(u->"cliente".equals(u.rol())).map(StatsUserClient.PublicUser::id).collect(Collectors.toSet());
    var adminIds=clients.stream().filter(u->"admin".equals(u.rol())).map(StatsUserClient.PublicUser::id).collect(Collectors.toSet());
    if(lines.stream().anyMatch(l->adminIds.contains(seller(l))))warnings.add("Existen ventas históricas de cuentas administradoras. Se conservan en ventas y no se cuentan como clientes vendedores.");
    long vendors=lines.stream().map(StatsCalculator::seller).filter(clientIds::contains).distinct().count();
    long buyers=approved.stream().map(o->o.payment.getUsuarioId()).filter(clientIds::contains).distinct().count();
    var users=new Users(clientIds.size(),vendors,buyers,clientIds.isEmpty()?0:100.0*vendors/clientIds.size());
    long old=os.stream().filter(o->o.payment.getEstado()==EstadoPago.PENDIENTE && Duration.between(o.payment.getFecha().atZone(StatsService.ZONE),now.atZone(StatsService.ZONE)).toHours()>=24).count();
    long uncredited=approved.stream().filter(o->!Boolean.TRUE.equals(o.payment.getLiquidado())).count();
    var alerts=new ArrayList<Alert>();
    if(old>0)alerts.add(new Alert("warning","Pagos pendientes hace 24 horas o más",old,"/admin/pagos?estado=PENDIENTE"));
    if(uncredited>0)alerts.add(new Alert("warning","Pagos aprobados con acreditación incompleta",uncredited,"/admin/pagos?estado=APROBADO"));
    for(var g:funnel)if(g.count()>0 && (g.key().equals("RECHAZADO")||g.key().equals("REEMBOLSADO")))alerts.add(new Alert("info",g.key().equals("RECHAZADO")?"Pagos rechazados en la selección":"Pagos reembolsados en la selección",g.count(),"/admin/pagos?estado="+g.key()));
    var sales=lines.stream().sorted(Comparator.<Line,LocalDateTime>comparing(l->l.payment.getFecha()).reversed().thenComparing(Line::productId)).map(l->new Sale(l.payment.getId(),l.productId,l.payment.getFecha().toString(),name(l),image(l),category(l),seller(l),l.payment.getMetodo().name(),l.net,l.gross,"APROBADO")).toList();
    return new StatsDto("America/Santiago",from==null?null:from.toString(),to.toString(),previous==null?null:prevFrom.toString(),previous==null?null:prevTo.toString(),current,previous,timeline,
      groupLines(lines,StatsCalculator::categoryKey,StatsCalculator::category),ranking(lines,false),ranking(lines,true),methods,funnel,
      groupProducts(created,Product::estado),groupProducts(created.stream().filter(p->p.motivoRechazo()!=null&&!p.motivoRechazo().isBlank()).toList(),Product::motivoRechazo),
      groupLines(lines,l->l.product==null?"Sin dato":Objects.toString(l.product.desgaste(),"Sin dato"),l->l.product==null?"Sin dato":Objects.toString(l.product.desgaste(),"Sin dato")),
      groupLines(lines,l->l.product==null?"Sin dato":Objects.toString(l.product.grado(),"Sin dato"),l->l.product==null?"Sin dato":Objects.toString(l.product.grado(),"Sin dato")),
      groupLines(lines,l->l.product==null||l.product.stattrak()==null?"Sin dato":l.product.stattrak()?"StatTrak":"Sin StatTrak",l->l.product==null||l.product.stattrak()==null?"Sin dato":l.product.stattrak()?"StatTrak":"Sin StatTrak"),attrs,users,alerts,sales,
      categories.stream().map(c->new Option(c.id(),c.nombre())).sorted(Comparator.comparing(Option::name)).toList(),warnings,errors,List.of());
  }
}
