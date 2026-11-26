package cl.skinspad.pago.stats;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import cl.skinspad.pago.domain.*;
import cl.skinspad.pago.escrow.Operacion;
import static cl.skinspad.pago.stats.StatsCatalogClient.*;

class StatsCalculatorTest {
  final LocalDate from=LocalDate.of(2026,11,1),to=LocalDate.of(2026,11,14);
  Product skin(long id,String name,long category,String type) {
    return new Product(id,name,"seller",new Category(category,category==1?"Rifles":"Pistolas"),type,"VENDIDO",from.atStartOfDay(),null,"mw","clasificado",false,new BigDecimal("0.12"),List.of());
  }
  Pago pay(long id,EstadoPago state,long gross,long... ids) {
    Pago p=new Pago();p.setId(id);p.setPedidoId("COMPRA-"+id);p.setUsuarioId("buyer");p.setEstado(state);p.setFecha(from.plusDays(2).atTime(13,0));p.setMontoTotal(BigDecimal.valueOf(gross));p.setMetodo(MetodoPago.SALDO);p.setLiquidado(true);
    for (long productId : ids) {
      var operacion = new Operacion();
      operacion.setProductoId(productId);
      operacion.setPrecio(BigDecimal.valueOf(100));
      p.agregarOperacion(operacion);
    }
    return p;
  }
  StatsDto stats(List<Pago> ps,List<Product> products,Long cat) {
    return StatsCalculator.calculate(ps,products,List.of(new Category(1L,"Rifles"),new Category(2L,"Pistolas")),List.of(new StatsUserClient.PublicUser("buyer","cliente"),new StatsUserClient.PublicUser("seller","cliente")),from,to,true,cat,null,to.atTime(12,0),Map.of());
  }
  @Test void unaOperacionPuedeContenerDosSkinsYTodosLosTotalesCuadran() {
    var dto=stats(List.of(pay(1,EstadoPago.APROBADO,210,1,2)),List.of(skin(1,"AK-47 | Redline",1,"rifle"),skin(2,"USP-S | Cortex",2,"pistola")),null);
    assertEquals(1,dto.current().operations());assertEquals(2,dto.current().units());
    assertEquals(210,dto.current().gross());assertEquals(200,dto.current().net());assertEquals(10,dto.current().commission());
    assertEquals(dto.current().units(),dto.categories().stream().mapToLong(StatsDto.Group::count).sum());
    assertEquals(dto.current().gross(),dto.timeline().stream().mapToLong(StatsDto.Bucket::gross).sum());
    assertEquals(dto.current().gross(),dto.methods().stream().mapToLong(StatsDto.Group::amount).sum());
    assertEquals(dto.current().units(),dto.sales().size());assertEquals(50,dto.users().sellerPercent());
  }
  @Test void noSeInfiereElEstadoDesdeElNombreYSeExcluyenOtrosEstados() {
    var dto=stats(List.of(pay(1,EstadoPago.APROBADO,105,1),pay(2,EstadoPago.PENDIENTE,105,2),pay(3,EstadoPago.REEMBOLSADO,105,3),pay(4,EstadoPago.RECHAZADO,105,4)),List.of(skin(1,"Skin pendiente (nombre libre)",1,"rifle")),null);
    assertEquals(1,dto.current().units());assertEquals(105,dto.current().gross());assertEquals(4,dto.funnel().stream().mapToLong(StatsDto.Group::count).sum());
  }
  @Test void categoriaEsFuenteUnicaAunqueTipoDiscrepe() {
    var dto=stats(List.of(pay(1,EstadoPago.APROBADO,105,1)),List.of(skin(1,"Skin",2,"francotirador")),null);
    assertEquals("Pistolas",dto.categories().get(0).label());assertTrue(dto.warnings().stream().anyMatch(w->w.contains("discordantes")));
  }
  @Test void repartoEnteroConservaTotalYFiltrosNoDuplicanImportes() {
    var p=pay(1,EstadoPago.APROBADO,211,1,2);var skins=List.of(skin(1,"A",1,"rifle"),skin(2,"B",2,"pistola"));
    var a=stats(List.of(p),skins,1L);var b=stats(List.of(p),skins,2L);
    assertEquals(211,a.current().gross()+b.current().gross());assertEquals(106,a.current().gross());assertEquals(105,b.current().gross());
    assertEquals(1,a.current().operations());assertEquals(1,b.current().operations());
  }
  @Test void noInventaComisionSinPrecioHistorico() {
    var p=pay(1,EstadoPago.APROBADO,105,1);p.getOperaciones().get(0).setPrecio(null);var dto=stats(List.of(p),List.of(skin(1,"A",1,"rifle")),null);
    assertEquals(105,dto.current().gross());assertNull(dto.current().net());assertNull(dto.current().commission());
  }
  @Test void conservaSkinsDeCatalogoFaltanteSinPerderUnidades() {
    var dto=stats(List.of(pay(1,EstadoPago.APROBADO,105,999)),List.of(),null);
    assertEquals(1,dto.current().units());assertEquals("Sin categoría disponible",dto.categories().get(0).label());
  }
  @Test void excluyeFixturesAunqueSeHayanAprobado() {
    var p=pay(1,EstadoPago.APROBADO,105,1);var dto=stats(List.of(p),List.of(skin(1,"[XAMPP] venta aprobada",1,"rifle")),null);assertEquals(0,dto.current().operations());
    p.setPedidoId("COMPRA-XAMPP-DEMO-V1-venta");assertEquals(0,stats(List.of(p),List.of(),null).current().operations());
  }
  @Test void limitesDeDiaIncluyenFinYComparanPeriodosIguales() {
    var current=pay(1,EstadoPago.APROBADO,105,1);current.setFecha(to.atTime(23,59,59));
    var previous=pay(2,EstadoPago.APROBADO,105,2);previous.setFecha(from.minusDays(1).atTime(23,59,59));
    var outside=pay(3,EstadoPago.APROBADO,105,3);outside.setFecha(to.plusDays(1).atStartOfDay());
    var dto=stats(List.of(current,previous,outside),List.of(),null);
    assertEquals(1,dto.current().operations());assertEquals(1,dto.previous().operations());assertEquals("2026-10-18",dto.compareFrom());
  }
  @Test void rankingNormalizaNombreYDesempataPorIngreso() {
    var p=pay(1,EstadoPago.APROBADO,420,1,2,3);p.getOperaciones().get(2).setPrecio(BigDecimal.valueOf(200));
    var dto=stats(List.of(p),List.of(skin(1," AK-47 | Redline ",1,"rifle"),skin(2,"ak-47 | redline",1,"rifle"),skin(3,"USP-S | Cortex",2,"pistola")),null);
    assertEquals(2,dto.skins().size());assertEquals(2,dto.skins().get(0).units());assertEquals(200,dto.skins().get(0).income());
  }
  @Test void vacioNoProducePorcentajesNiTicketFicticios() {
    var dto=stats(List.of(),List.of(),null);assertEquals(0,dto.current().gross());assertNull(dto.current().ticket());assertNull(dto.current().conversion());assertNull(dto.attributes().floatAverage());
  }
  @Test void alertaUsaHorasRealesEnCambioHorarioSantiago() {
    var p=pay(1,EstadoPago.PENDIENTE,105,1);p.setFecha(LocalDateTime.of(2026,9,5,12,0));
    var dto=StatsCalculator.calculate(List.of(p),List.of(),List.of(),List.of(),LocalDate.of(2026,9,1),LocalDate.of(2026,9,6),false,null,null,LocalDateTime.of(2026,9,6,12,0),Map.of());
    assertTrue(dto.alerts().isEmpty(),"El salto de horario hace que solo hayan pasado 23 horas");
  }
}
