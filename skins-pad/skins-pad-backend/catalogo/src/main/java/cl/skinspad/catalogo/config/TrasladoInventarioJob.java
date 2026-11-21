package cl.skinspad.catalogo.config;

import cl.skinspad.catalogo.service.TrasladoInventarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;

@Component
@EnableScheduling
@ConditionalOnProperty(name = "CATALOGO_TRASLADO_INICIAL", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class TrasladoInventarioJob {
  private final TrasladoInventarioService traslado;
  private volatile boolean iniciado;

  @EventListener(ApplicationReadyEvent.class)
  public void habilitar() {
    iniciado = true;
  }

  @Scheduled(fixedDelay = 3000)
  public void trasladarPendientes() {
    if (!iniciado) return;
    for (Long id : traslado.pendientes()) {
      try {
        traslado.trasladar(id);
      } catch (ResourceAccessException e) {
        log.debug("El traslado se reintentará cuando inventario esté disponible.", e);
        return;
      } catch (RuntimeException e) {
        log.warn("No se pudo trasladar la publicación {}. Se reintentará.", id, e);
      }
    }
  }
}
