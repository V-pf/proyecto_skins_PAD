package cl.skinspad.pago.config;

import cl.skinspad.pago.escrow.CustodiaMigracion;
import cl.skinspad.pago.escrow.Operacion;
import cl.skinspad.pago.repository.PagoRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Mismo punto de arranque que UsuarioSeed y CatalogoSeed; Hibernate ya creó las tablas. */
@Component
@Order(1)
@RequiredArgsConstructor
public class CustodiaSeed implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(CustodiaSeed.class);
    private final PagoRepository pagos;
    private final CustodiaMigracion migracion;

    @Override
    public void run(String... args) { cargarPendientes(); }

    public void cargarPendientes() {
        for (Long id : pagos.buscarHistoricosPorImportar(Operacion.PENDIENTE_IMPORTACION)) {
            try {
                migracion.importar(id);
            } catch (feign.FeignException dependencia) {
                log.warn("Importación de custodia del pago {} pendiente: catálogo no disponible (HTTP {}). Se reintentará.", id, dependencia.status());
                // Si catálogo aún está arrancando, no insistir una vez por cada pago.
                break;
            } catch (RuntimeException error) {
                log.error("No se pudo importar custodia del pago {}. Se conserva el registro original para reintentar.", id, error);
            }
        }
    }
}
