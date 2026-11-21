package cl.skinspad.reporte.soporte;
@org.springframework.stereotype.Component @lombok.RequiredArgsConstructor public class PrecioJob {
 private final PrecioService precios;
 @org.springframework.scheduling.annotation.Scheduled(fixedDelayString="${soporte.precio-job-ms:900000}",initialDelayString="${soporte.job-inicial-ms:60000}") public void ejecutar(){try{precios.analizar();}catch(RuntimeException e){org.slf4j.LoggerFactory.getLogger(getClass()).warn("Análisis de precios pendiente: {}",e.getClass().getSimpleName());}}
}
