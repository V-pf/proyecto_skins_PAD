package cl.skinspad.pago.escrow;
import cl.skinspad.pago.client.*;import cl.skinspad.pago.domain.*;import cl.skinspad.pago.dto.CheckoutRequest;import cl.skinspad.pago.repository.PagoRepository;
import org.junit.jupiter.api.*;import org.springframework.beans.factory.annotation.Autowired;import org.springframework.boot.test.context.SpringBootTest;import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;import java.util.concurrent.*;import java.math.BigDecimal;import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;import static org.mockito.Mockito.*;import static cl.skinspad.pago.escrow.MaquinaOperacion.Accion.*;import static cl.skinspad.pago.escrow.EstadoOperacion.*;
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:escrow;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000","spring.datasource.driver-class-name=org.h2.Driver","spring.jpa.hibernate.ddl-auto=create-drop","escrow.job-initial-ms=3600000"})
class CustodiaIntegrationTest {
 @Autowired CustodiaMigracion migracion;
 @Autowired cl.skinspad.pago.config.CustodiaSeed seed;
 @Autowired cl.skinspad.pago.config.MigracionDetallePago detallePago;
 @Autowired JdbcTemplate jdbc;
 @Autowired CustodiaCheckout checkout;@Autowired CustodiaService service;@Autowired OperacionRepository ops;@Autowired PagoRepository pagos;@Autowired AsientoRepository ledger;@Autowired EventoRepository eventos;@Autowired CustodiaJob job;
 @Autowired org.springframework.test.web.servlet.MockMvc mvc;
 @Autowired cl.skinspad.pago.stats.StatsService stats;
 @MockitoBean cl.skinspad.pago.stats.StatsUserClient sessions;
 @MockitoBean cl.skinspad.pago.stats.StatsCatalogClient statsCatalog;
 @MockitoBean UsuarioClient users;@MockitoBean CatalogoPagoClient catalog;
 final Map<String,Long> abonos=new ConcurrentHashMap<>();final Map<String,Long> debitos=new ConcurrentHashMap<>();
 @BeforeEach void limpiar(){eventos.deleteAll();ledger.deleteAll();ops.deleteAll();pagos.deleteAll();abonos.clear();debitos.clear();
 jdbc.execute("DROP TABLE IF EXISTS pago_producto_ids, pago_precios");
 when(users.buscar(anyString())).thenAnswer(x->new UsuarioRemoto(x.getArgument(0),BigDecimal.valueOf(100000),false));
 when(catalog.reservar(any())).thenAnswer(x->{CatalogoPagoClient.Reserva r=x.getArgument(0);return r.ids().stream().map(id->new CatalogoPagoClient.Skin(id,"seller",BigDecimal.valueOf(1000),"Skin "+id)).toList();});
 doAnswer(x->{abonos.putIfAbsent(x.getArgument(2),((BigDecimal)x.getArgument(1)).longValueExact());return null;}).when(users).abonar(anyString(),any(),anyString());
 doAnswer(x->{debitos.putIfAbsent(x.getArgument(2),((BigDecimal)x.getArgument(1)).longValueExact());return null;}).when(users).debitar(anyString(),any(),anyString());
 }
 Operacion nueva(){var p=checkout.iniciar("buyer",new CheckoutRequest(UUID.randomUUID().toString(),List.of(1L),MetodoPago.SALDO));return ops.findByPagoIdOrderById(p.getId()).get(0);}
 void action(Operacion o,MaquinaOperacion.Accion a,String actor){service.accion(o.getId(),a,actor,actor.equals("admin")?"admin":"cliente","Motivo de prueba",false);}
 void entregar(Operacion o){action(o,ACEPTAR,"seller");action(o,ENTREGAR,"seller");}
 void vencer(Operacion o){var x=ops.findById(o.getId()).orElseThrow();x.setVence(Instant.now().minusSeconds(1));ops.saveAndFlush(x);job.ejecutar();}
 EstadoOperacion estado(Operacion o){return ops.findById(o.getId()).orElseThrow().getEstado();}
 Pago historico(EstadoPago estado,boolean liquidado){
  var p=new Pago();p.setPedidoId("COMPRA-historica-"+UUID.randomUUID());p.setUsuarioId("buyer");p.setMetodo(MetodoPago.SALDO);p.setEstado(estado);p.setLiquidado(liquidado);p.setFecha(java.time.LocalDateTime.of(2026,10,1,12,0));p.setMontoTotal(BigDecimal.valueOf(3150));
  p = pagos.saveAndFlush(p);
  crearTablasAnteriores();
  guardarDetalleAnterior(p.getId(), 1L, BigDecimal.valueOf(1000));
  guardarDetalleAnterior(p.getId(), 2L, BigDecimal.valueOf(2000));
  when(catalog.buscar(1L)).thenReturn(new CatalogoPagoClient.Skin(1L,"seller",BigDecimal.valueOf(9999),"AK-47"));when(catalog.buscar(2L)).thenReturn(new CatalogoPagoClient.Skin(2L,"seller",BigDecimal.valueOf(9999),"AWP"));return p;
 }
 void crearTablasAnteriores() {
  jdbc.execute("CREATE TABLE IF NOT EXISTS pago_producto_ids (pago_id BIGINT NOT NULL, producto_ids BIGINT)");
  jdbc.execute("CREATE TABLE IF NOT EXISTS pago_precios (pago_id BIGINT NOT NULL, producto_id BIGINT NOT NULL, precio_venta DECIMAL(15,2), PRIMARY KEY (pago_id, producto_id))");
 }
 void guardarDetalleAnterior(Long pagoId, Long productoId, BigDecimal precio) {
  jdbc.update("INSERT INTO pago_producto_ids VALUES (?, ?)", pagoId, productoId);
  jdbc.update("INSERT INTO pago_precios VALUES (?, ?, ?)", pagoId, productoId, precio);
 }
 void consolidarTablas() { detallePago.run(new DefaultApplicationArguments()); }
 void importarHistoricos() { consolidarTablas(); seed.run(); }
 int tablasAnteriores() {
  return jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='PUBLIC' AND table_name IN ('PAGO_PRODUCTO_IDS', 'PAGO_PRECIOS')", Integer.class);
 }
 @Test void seedBaseVaciaNoCreaComprasNiDinero(){seed.run();assertEquals(0,ops.count());assertEquals(0,ledger.count());verify(catalog,never()).buscar(anyLong());verify(users,never()).abonar(anyString(),any(),anyString());}
 @Test void seedImportaHistoricoUnaVezSinReabonar(){var p=historico(EstadoPago.APROBADO,true);importarHistoricos();importarHistoricos();var lineas=ops.findByPagoIdOrderById(p.getId());assertEquals(0,tablasAnteriores());assertEquals(2,lineas.size());assertTrue(lineas.stream().allMatch(o->o.isHistorica()&&o.getEstado()==COMPLETADA&&!o.isRetenido()));assertEquals(3150,lineas.stream().mapToLong(o->o.precioEnPesos()+o.getComision()).sum());assertEquals(150,lineas.stream().mapToLong(Operacion::getComision).sum());assertEquals(2,eventos.count());assertEquals(0,ledger.count());assertTrue(abonos.isEmpty());assertTrue(debitos.isEmpty());assertEquals(p.getFecha().atZone(java.time.ZoneId.of("America/Santiago")).toInstant(),lineas.get(0).getCreada());}
 @Test void seedPreparaPendienteSinCobrarNiCambiarReferencia(){var p=historico(EstadoPago.PENDIENTE,false);importarHistoricos();var guardado=pagos.findById(p.getId()).orElseThrow();assertTrue(guardado.getCustodia());assertEquals("RESERVAR",guardado.getEtapaCustodia());assertEquals(p.getPedidoId(),guardado.getPedidoId());assertTrue(ops.findByPagoIdOrderById(p.getId()).stream().allMatch(o->o.getEstado()==PAGO_PENDIENTE&&!o.isHistorica()&&o.getVence()!=null));assertTrue(debitos.isEmpty());seed.run();assertEquals(2,ops.count());}
 @Test void seedConservaElDetalleSiCatalogoEstaCaido() {
  var p = historico(EstadoPago.APROBADO, true);
  var caida = new feign.FeignException.ServiceUnavailable("Catálogo temporalmente apagado",
      feign.Request.create(feign.Request.HttpMethod.GET, "http://catalogo/api/productos/2", Map.of(),
          (byte[]) null, java.nio.charset.StandardCharsets.UTF_8, null), null, Map.of());
  when(catalog.buscar(2L)).thenThrow(caida);
  importarHistoricos();
  var guardado = pagos.findById(p.getId()).orElseThrow();
  assertEquals(0, tablasAnteriores());
  assertEquals(List.of(1L, 2L), guardado.getProductoIds());
  assertEquals(new BigDecimal("2000.00"), guardado.getPrecios().get(2L));
  assertTrue(guardado.getOperaciones().stream().allMatch(o -> CONGELADA.equals(o.getEstado())));
  assertEquals(0, eventos.count());
  doReturn(new CatalogoPagoClient.Skin(2L, "seller", BigDecimal.TEN, "AWP")).when(catalog).buscar(2L);
  seed.run();
  assertEquals(2, eventos.count());
  assertEquals(guardado.getOperaciones().stream().map(Operacion::getId).toList(),
      ops.findByPagoIdOrderById(p.getId()).stream().map(Operacion::getId).toList());
 }
 @Test void seedConservaPreciosDecimalesYAceptaPrecioDesconocido() {
  jdbc.execute("ALTER TABLE operaciones ALTER COLUMN precio BIGINT NOT NULL");
  jdbc.execute("ALTER TABLE operaciones ADD COLUMN url_intercambio VARCHAR(255)");
  var p = historico(EstadoPago.PENDIENTE, false);
  jdbc.update("DELETE FROM pago_precios WHERE pago_id=? AND producto_id=1", p.getId());
  jdbc.update("UPDATE pago_precios SET precio_venta=1999.25 WHERE pago_id=? AND producto_id=2", p.getId());
  importarHistoricos();
  var guardado = pagos.findById(p.getId()).orElseThrow();
  assertEquals(List.of(1L, 2L), guardado.getProductoIds());
  assertEquals(Map.of(2L, new BigDecimal("1999.25")), guardado.getPrecios());
  assertTrue(guardado.getOperaciones().stream().allMatch(o -> o.getEstado()==CONGELADA && o.isHistorica()));
  assertNull(guardado.getCustodia());
  assertEquals(0, ledger.count());
  assertEquals(0, jdbc.queryForObject("""
      SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = 'PUBLIC' AND table_name = 'OPERACIONES' AND column_name = 'URL_INTERCAMBIO'
      """, Integer.class));
 }
 @Test void seedRespetaImportacionManualPrevia(){var p=historico(EstadoPago.APROBADO,true);consolidarTablas();migracion.importar(p.getId());var o=ops.findByPagoIdOrderById(p.getId()).get(0);o.setCausa("REVISION_MANUAL");ops.saveAndFlush(o);migracion.importar(p.getId());assertEquals("REVISION_MANUAL",ops.findById(o.getId()).orElseThrow().getCausa());assertEquals(2,ops.count());}
 @Test void consolidacionRespetaUnaOperacionEnCustodiaYConservaSuHistorial() throws Exception {
  var o = nueva();
  long eventosAntes = eventos.count();
  long asientosAntes = ledger.count();
  crearTablasAnteriores();
  guardarDetalleAnterior(o.getPagoId(), o.getProductoId(), o.getPrecio());
  importarHistoricos();
  assertEquals(0, tablasAnteriores());
  assertEquals(o.getId(), ops.findByPagoIdOrderById(o.getPagoId()).get(0).getId());
  assertEquals(eventosAntes, eventos.count());
  assertEquals(asientosAntes, ledger.count());
  when(sessions.validate("buyer")).thenReturn(new cl.skinspad.pago.stats.StatsUserClient.Identity("buyer", "cliente", false));
  mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/pagos/"+o.getPagoId())
      .header("Authorization", "Bearer buyer"))
      .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
      .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.productoIds[0]").value(1))
      .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.precios.1").value(1000.0))
      .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.operaciones").doesNotExist());
 }
 @Test void endpointsNoConfianEnHeadersYRechazanIdAjeno()throws Exception{
  var o=nueva();
  mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/operaciones/"+o.getId()).header("X-Skins-Usuario","buyer")).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isUnauthorized());
  when(sessions.validate("otro")).thenReturn(new cl.skinspad.pago.stats.StatsUserClient.Identity("otro","cliente",false));
  mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/operaciones/"+o.getId()).header("Authorization","Bearer otro")).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isForbidden());
  when(sessions.validate("buyer")).thenReturn(new cl.skinspad.pago.stats.StatsUserClient.Identity("buyer","cliente",false));
  mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/operaciones/"+o.getId()+"/aceptar").header("Authorization","Bearer buyer")).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isForbidden());
  mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/pagos/"+o.getPagoId()).header("X-Skins-Usuario","buyer")).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isUnauthorized());
 }
 @Test void estadisticasNoReconocenCustodiaComoIngreso(){
  when(sessions.validate("admin")).thenReturn(new cl.skinspad.pago.stats.StatsUserClient.Identity("admin","admin",false));
  when(statsCatalog.products()).thenReturn(List.of());when(statsCatalog.categories()).thenReturn(List.of());when(sessions.users()).thenReturn(List.of());
  var o=nueva();var antes=stats.get("Bearer admin",null,null,false,null,null);assertEquals(0,antes.current().gross());assertEquals(1050,antes.custody().stream().filter(g->g.key().equals("EN_CUSTODIA")).findFirst().orElseThrow().amount());
  entregar(o);action(o,RECIBIR,"buyer");var despues=stats.get("Bearer admin",null,null,false,null,null);assertEquals(1050,despues.current().gross());assertEquals(50,despues.current().commission());assertEquals(1,despues.current().units());assertEquals(despues.current().units(),despues.categories().stream().mapToLong(g->g.count()).sum());
 }
 @Test void pendienteExternoAnteriorVenceSinAbono() {
  var p = historico(EstadoPago.PENDIENTE, false);
  p.setMetodo(MetodoPago.MERCADO_PAGO);
  jdbc.update("UPDATE pago_precios SET precio_venta=1000 WHERE pago_id=? AND producto_id=2", p.getId());
  p.setMontoTotal(BigDecimal.valueOf(2100));
  pagos.saveAndFlush(p);
  importarHistoricos();
  service.pago(p.getId());
  var operaciones = ops.findByPagoIdOrderById(p.getId());
  assertEquals(2, operaciones.size());
  assertTrue(operaciones.stream().allMatch(o -> o.getEstado() == PAGO_PENDIENTE));
  assertThrows(org.springframework.web.server.ResponseStatusException.class,
      () -> action(operaciones.get(0), CANCELAR, "admin"));
  operaciones.forEach(this::vencer);
  assertTrue(operaciones.stream().allMatch(o -> estado(o) == CANCELADA));
  assertTrue(abonos.isEmpty());
  assertTrue(debitos.isEmpty());
 }
 @Test void felizSoloLiberaTrasRecepcion(){var o=nueva();assertEquals(PAGO_RETENIDO,estado(o));assertTrue(abonos.isEmpty());assertEquals(1050,debitos.values().stream().mapToLong(Long::longValue).sum());entregar(o);action(o,RECIBIR,"buyer");assertEquals(COMPLETADA,estado(o));assertEquals(1000,abonos.values().stream().mapToLong(Long::longValue).sum());assertEquals(3,ledger.count());}
 @Test void vendedorRechaza(){var o=nueva();action(o,RECHAZAR,"seller");assertEquals(REEMBOLSADA,estado(o));assertEquals(1050,abonos.values().stream().mapToLong(Long::longValue).sum());}
 @Test void vendedorNoResponde(){var o=nueva();vencer(o);assertEquals(REEMBOLSADA,estado(o));}
 @Test void vendedorNoEntrega(){var o=nueva();action(o,ACEPTAR,"seller");vencer(o);assertEquals(REEMBOLSADA,estado(o));}
 @Test void compradorNoConfirma(){var o=nueva();entregar(o);vencer(o);assertEquals(COMPLETADA,estado(o));}
 @Test void disputaComprador(){var o=nueva();entregar(o);action(o,DISPUTAR,"buyer");action(o,RESOLVER_COMPRADOR,"admin");assertEquals(REEMBOLSADA,estado(o));}
 @Test void disputaVendedor(){var o=nueva();entregar(o);action(o,DISPUTAR,"buyer");action(o,RESOLVER_VENDEDOR,"admin");assertEquals(COMPLETADA,estado(o));}
 @Test void congelarBloqueaJobYExigeMotivo(){var o=nueva();entregar(o);assertThrows(IllegalArgumentException.class,()->service.accion(o.getId(),CONGELAR,"admin","admin","",false));action(o,CONGELAR,"admin");vencer(o);assertEquals(CONGELADA,estado(o));assertTrue(abonos.isEmpty());}
 @Test void propiedadYRol(){var o=nueva();assertThrows(org.springframework.web.server.ResponseStatusException.class,()->action(o,ACEPTAR,"otro"));assertThrows(org.springframework.web.server.ResponseStatusException.class,()->action(o,ACEPTAR,"admin"));assertThrows(org.springframework.web.server.ResponseStatusException.class,()->action(o,CONGELAR,"seller"));entregar(o);assertThrows(org.springframework.web.server.ResponseStatusException.class,()->action(o,RECIBIR,"seller"));}
 void paralelo(Runnable task)throws Exception{var pool=Executors.newFixedThreadPool(2);try{var gate=new CountDownLatch(1);var a=pool.submit(()->{try{gate.await();task.run();}catch(InterruptedException e){throw new RuntimeException(e);}});var b=pool.submit(()->{try{gate.await();task.run();}catch(InterruptedException e){throw new RuntimeException(e);}});gate.countDown();a.get(15,TimeUnit.SECONDS);b.get(15,TimeUnit.SECONDS);}finally{pool.shutdownNow();}}
 @Test void dobleLiberacion()throws Exception{var o=nueva();entregar(o);paralelo(()->action(o,RECIBIR,"buyer"));assertEquals(1,abonos.size());assertEquals(3,ledger.count());}
 @Test void dobleReembolso()throws Exception{var o=nueva();paralelo(()->action(o,RECHAZAR,"seller"));assertEquals(1,abonos.size());assertEquals(2,ledger.count());}
 @Test void recuperaAbonoConfirmadoConRespuestaPerdida(){var o=nueva();entregar(o);var once=new java.util.concurrent.atomic.AtomicBoolean();doAnswer(x->{abonos.putIfAbsent(x.getArgument(2),((BigDecimal)x.getArgument(1)).longValueExact());if(!once.getAndSet(true))throw new IllegalStateException("respuesta perdida");return null;}).when(users).abonar(anyString(),any(),anyString());action(o,RECIBIR,"buyer");assertEquals(ENTREGADA,estado(o));assertNotNull(ops.findById(o.getId()).orElseThrow().getAccionPendiente());assertThrows(IllegalStateException.class,()->action(o,DISPUTAR,"buyer"));job.ejecutar();assertEquals(COMPLETADA,estado(o));assertEquals(1,abonos.size());assertEquals(3,ledger.count());}
 @Test void comisionSeDistribuyeSinPerderPesos(){var p=checkout.iniciar("buyer",new CheckoutRequest("multi",List.of(1L,2L),MetodoPago.SALDO));var lista=ops.findByPagoIdOrderById(p.getId());assertEquals(p.getMontoTotal().longValueExact(),lista.stream().mapToLong(o->o.precioEnPesos()+o.getComision()).sum());assertEquals(1,debitos.size());}
 @Test void pagoSimuladoYaRegistradoPuedeCerrarseSinMovimientosDeSaldo() {
  var anterior = historico(EstadoPago.PENDIENTE, false);
  anterior.setMetodo(MetodoPago.TRANSFERENCIA);
  anterior.setSimulado(true);
  jdbc.update("UPDATE pago_precios SET precio_venta=1000 WHERE pago_id=? AND producto_id=2", anterior.getId());
  anterior.setMontoTotal(BigDecimal.valueOf(2100));
  pagos.saveAndFlush(anterior);
  importarHistoricos();
  var p = checkout.iniciar("buyer", new CheckoutRequest(anterior.getPedidoId().substring("COMPRA-".length()),
      List.of(1L, 2L), MetodoPago.TRANSFERENCIA));
  assertTrue(p.getSimulado());
  assertEquals(EstadoPago.APROBADO, p.getEstado());
  var lista = ops.findByPagoIdOrderById(p.getId());
  assertEquals(2, lista.size());
  assertTrue(lista.stream().allMatch(o -> o.getEstado() == PAGO_RETENIDO && !o.isRetenido()));
  var entregada = lista.get(0);
  entregar(entregada);
  action(entregada, RECIBIR, "buyer");
  assertEquals(COMPLETADA, estado(entregada));
  var rechazada = lista.get(1);
  action(rechazada, RECHAZAR, "seller");
  assertEquals(REEMBOLSADA, estado(rechazada));
  assertEquals(0, ledger.count());
  assertTrue(abonos.isEmpty());
  assertTrue(debitos.isEmpty());
  assertTrue(ops.findVentasLiquidadasDeVendedor("seller").isEmpty());
  assertTrue(ops.findByEstadoAndCompletadaGreaterThanEqual(COMPLETADA, Instant.now().minusSeconds(600)).isEmpty());
 }

 @Test void unaCompraNuevaRequiereSaldoAunqueIncluyaSkinsPrecargadas() {
  for (var metodo : List.of(MetodoPago.TARJETA, MetodoPago.TRANSFERENCIA, MetodoPago.MERCADO_PAGO)) {
   var compra = new CheckoutRequest("mezclada-" + metodo, List.of(1L, 100L), metodo);
   assertThrows(IllegalArgumentException.class, () -> checkout.iniciar("buyer", compra));
  }
  verifyNoInteractions(catalog, users);
  assertEquals(0, pagos.count());
 }

 @Test void unaCotizacionDeOtraPublicacionNoCreaUnPago() {
  doReturn(List.of(new CatalogoPagoClient.Skin(2L, "seller", BigDecimal.valueOf(1000), "Skin 2")))
      .when(catalog).reservar(any());
  assertThrows(IllegalStateException.class, () -> checkout.iniciar("buyer",
      new CheckoutRequest("cotizacion-ajena", List.of(1L), MetodoPago.SALDO)));
  assertEquals(0, pagos.count());
  assertEquals(0, ops.count());
  assertTrue(debitos.isEmpty());
 }
}
