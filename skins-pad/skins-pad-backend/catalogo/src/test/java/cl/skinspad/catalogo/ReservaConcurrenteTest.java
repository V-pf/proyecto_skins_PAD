package cl.skinspad.catalogo;
import cl.skinspad.catalogo.domain.*;import cl.skinspad.catalogo.repository.*;import cl.skinspad.catalogo.service.CatalogoService;import cl.skinspad.catalogo.dto.ReservaRequest;
import org.junit.jupiter.api.Test;import org.springframework.beans.factory.annotation.Autowired;import org.springframework.boot.test.context.SpringBootTest;import java.util.*;import java.util.concurrent.*;import java.math.BigDecimal;import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:reservas;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000","spring.datasource.driver-class-name=org.h2.Driver","spring.jpa.hibernate.ddl-auto=create-drop"})
class ReservaConcurrenteTest {
 @Autowired ProductoRepository productos;@Autowired CategoriaRepository categorias;@Autowired CatalogoService service;
 @Test void unaSkinSoloTieneUnComprador()throws Exception{
 var c=categorias.findAll().stream().filter(x->x.getNombre().equals("Rifles")).findFirst().orElseThrow();var p=new Producto();p.setCategoria(c);p.setNombre("AK-47");p.setPrecio(BigDecimal.valueOf(1000));p.setVendedorId("seller");p.setEstado(EstadoProducto.ACTIVO);p=productos.saveAndFlush(p);final Long id=p.getId();
 var gate=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);var wins=new java.util.concurrent.atomic.AtomicInteger();var conflicts=new java.util.concurrent.atomic.AtomicInteger();
 try{var futures=new ArrayList<Future<?>>();for(String buyer:List.of("A","B"))futures.add(pool.submit(()->{try{gate.await();service.reservar(new ReservaRequest(buyer,List.of(id),"RESERVAR"));wins.incrementAndGet();}catch(IllegalStateException e){conflicts.incrementAndGet();}catch(InterruptedException e){throw new RuntimeException(e);}}));gate.countDown();for(var f:futures)f.get(15,TimeUnit.SECONDS);}finally{pool.shutdownNow();}
 assertEquals(1,wins.get());assertEquals(1,conflicts.get());assertEquals(EstadoProducto.INACTIVO,productos.findById(id).orElseThrow().getEstado());
 assertThrows(IllegalStateException.class,()->service.actualizarEstado(id,EstadoProducto.ACTIVO));assertThrows(IllegalStateException.class,()->service.actualizarPrecio(id,BigDecimal.valueOf(1)));
 }
}
