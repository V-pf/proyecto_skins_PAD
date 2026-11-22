package cl.skinspad.catalogo;

import cl.skinspad.catalogo.domain.*;
import cl.skinspad.catalogo.repository.*;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:moderacion;MODE=MySQL;DB_CLOSE_DELAY=-1","spring.datasource.driver-class-name=org.h2.Driver","spring.jpa.hibernate.ddl-auto=create-drop"})
@AutoConfigureMockMvc
class ModeracionPublicacionesTest {
    static final HttpServer usuarios = iniciarUsuarios();
    @Autowired MockMvc mvc;
    @Autowired ProductoRepository productos;
    @Autowired CategoriaRepository categorias;

    static HttpServer iniciarUsuarios() {
        try {
            var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
            server.createContext("/api/auth/validar", exchange -> {
                boolean admin="token=admin".equals(exchange.getRequestURI().getQuery());
                String body=admin?"{\"id\":\"admin\",\"rol\":\"admin\",\"suspendido\":false}":"{\"id\":\"vendedor\",\"rol\":\"cliente\",\"suspendido\":false}";
                byte[] bytes=body.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type","application/json");
                exchange.sendResponseHeaders(200,bytes.length);
                exchange.getResponseBody().write(bytes);exchange.close();
            });
            server.start();return server;
        } catch(java.io.IOException e) {throw new IllegalStateException(e);}
    }
    @DynamicPropertySource static void configurar(DynamicPropertyRegistry properties) {
        properties.add("USUARIO_URL",()->"http://127.0.0.1:"+usuarios.getAddress().getPort());
    }
    @AfterAll static void cerrarUsuarios() {usuarios.stop(0);}
    Producto activa(boolean importada) {
        var p=new Producto();p.setNombre("AK-47 | Redline");p.setVendedorId("vendedor");p.setVendedor("Vendedor");
        p.setCategoria(categorias.findAll().get(0));p.setPrecio(BigDecimal.valueOf(1000));
        p.setEstado(EstadoProducto.ACTIVO);
        if(importada)p.setInventarioId(123456L);
        return productos.saveAndFlush(p);
    }
    @Test void adminMarcaComoAptaUnaPublicacionYaActiva() throws Exception {
        for(boolean importada: new boolean[]{false,true}) {
            var p=activa(importada);
            mvc.perform(patch("/api/productos/"+p.getId()+"/estado").header("Authorization","Bearer admin")
                .contentType("application/json").content("{\"estado\":\"ACTIVO\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("ACTIVO"))
                .andExpect(jsonPath("$.revisionApta").value(true));
            var guardada=productos.findById(p.getId()).orElseThrow();
            assertNull(guardada.getMotivoRechazo());assertEquals("vendedor",guardada.getVendedorId());
            assertEquals(p.getInventarioId(),guardada.getInventarioId());
        }
    }
    @Test void adminRetiraUnaPublicacionNoAptaConMotivo() throws Exception {
        var p=activa(false);
        mvc.perform(patch("/api/productos/"+p.getId()+"/estado").header("Authorization","Bearer admin")
            .contentType("application/json").content("{\"estado\":\"RECHAZADO\",\"motivoRechazo\":\"Precio incorrecto\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("RECHAZADO"))
            .andExpect(jsonPath("$.revisionApta").value(false));
        assertEquals("Precio incorrecto",productos.findById(p.getId()).orElseThrow().getMotivoRechazo());
    }
    @Test void clienteNoPuedeModerarSuPropiaPublicacion() throws Exception {
        var p=activa(false);
        mvc.perform(patch("/api/productos/"+p.getId()+"/estado").header("Authorization","Bearer cliente")
            .contentType("application/json").content("{\"estado\":\"ACTIVO\"}"))
            .andExpect(status().isForbidden());
        assertNull(productos.findById(p.getId()).orElseThrow().getRevisionApta());
    }
    @Test void unaPublicacionReservadaNoSePuedeRetirar() throws Exception {
        var p=activa(false);p.setReservaPago("pago-1");productos.saveAndFlush(p);
        mvc.perform(patch("/api/productos/"+p.getId()+"/estado").header("Authorization","Bearer admin")
            .contentType("application/json").content("{\"estado\":\"RECHAZADO\",\"motivoRechazo\":\"Precio incorrecto\"}"))
            .andExpect(status().isConflict());
    }
}
