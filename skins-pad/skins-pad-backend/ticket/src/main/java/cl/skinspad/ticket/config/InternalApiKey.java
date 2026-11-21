package cl.skinspad.ticket.config;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/** Credencial compartida de desarrollo local, independiente de cómo se arranque Java. */
@Component
public class InternalApiKey {
    private final String key;

    public InternalApiKey(Environment env) {
        String configured = env.getProperty("INTERNAL_API_KEY", "").trim();
        if (!configured.isEmpty()) {
            key = configured;
            return;
        }
        String address = env.getProperty("server.address", "");
        if (!java.util.Set.of("127.0.0.1", "localhost", "::1").contains(address)) {
            throw new IllegalStateException("Configura INTERNAL_API_KEY para servicios fuera de localhost");
        }
        Path root = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (root != null && !(Files.exists(root.resolve("pago/pom.xml"))
                && Files.exists(root.resolve("usuario/pom.xml"))
                && Files.exists(root.resolve("catalogo/pom.xml")))) {
            // También admite ejecutar desde la raíz que contiene skins-pad-backend.
            Path backend = root.resolve("skins-pad-backend");
            if (Files.exists(backend.resolve("pago/pom.xml"))
                    && Files.exists(backend.resolve("usuario/pom.xml"))
                    && Files.exists(backend.resolve("catalogo/pom.xml"))) {
                root = backend;
                break;
            }
            root = root.getParent();
        }
        if (root == null) throw new IllegalStateException("Configura INTERNAL_API_KEY: no se encontró la carpeta de los microservicios");
        key = loadLocalKey(root.resolve(".internal-api-key"));
    }

    public String value() { return key; }

    // El bloqueo impide que dos servicios generen claves distintas al arrancar juntos.
    static synchronized String loadLocalKey(Path file) {
        try (var channel = FileChannel.open(file, StandardOpenOption.CREATE,
                StandardOpenOption.READ, StandardOpenOption.WRITE);
             var lock = channel.lock()) {
            if (channel.size() == 0) {
                byte[] random = new byte[32];
                new SecureRandom().nextBytes(random);
                var bytes = ByteBuffer.wrap(Base64.getEncoder().encode(random));
                while (bytes.hasRemaining()) channel.write(bytes);
                channel.force(true);
            }
            if (channel.size() > 4096) throw new IllegalStateException("Archivo de credencial interna inválido");
            channel.position(0);
            var bytes = ByteBuffer.allocate((int) channel.size());
            while (bytes.hasRemaining() && channel.read(bytes) != -1) { }
            String value = new String(bytes.array(), StandardCharsets.UTF_8).trim();
            if (value.length() < 32) throw new IllegalStateException("La credencial interna local es demasiado corta");
            return value;
        } catch (IOException error) {
            throw new IllegalStateException("No se pudo leer o crear la credencial interna local", error);
        }
    }
}
