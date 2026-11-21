package cl.skinspad.pago.config;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.env.MockEnvironment;
import java.nio.file.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
class InternalApiKeyTest {
 @TempDir Path dir;
 @Test void creaUnaClaveYLaConserva(){var file=dir.resolve(".internal-api-key");var first=InternalApiKey.loadLocalKey(file);assertEquals(44,first.length());assertEquals(first,InternalApiKey.loadLocalKey(file));}
 @Test void arranquesSimultaneosCompartenClave()throws Exception{var file=dir.resolve(".internal-api-key");var pool=Executors.newFixedThreadPool(2);try{var a=pool.submit(()->InternalApiKey.loadLocalKey(file));var b=pool.submit(()->InternalApiKey.loadLocalKey(file));assertEquals(a.get(5,TimeUnit.SECONDS),b.get(5,TimeUnit.SECONDS));}finally{pool.shutdownNow();}}
 @Test void configuracionExplicitaTienePrioridad(){assertEquals("clave-explicita",new InternalApiKey(new MockEnvironment().withProperty("INTERNAL_API_KEY","clave-explicita")).value());}
 @Test void noGeneraClaveDeDesarrolloEnInterfacesPublicas(){assertThrows(IllegalStateException.class,()->new InternalApiKey(new MockEnvironment().withProperty("server.address","0.0.0.0")));}
 @Test void noSobrescribeUnaClaveInvalida()throws Exception{var file=dir.resolve(".internal-api-key");Files.writeString(file,"corta");assertThrows(IllegalStateException.class,()->InternalApiKey.loadLocalKey(file));assertEquals("corta",Files.readString(file));}
}
