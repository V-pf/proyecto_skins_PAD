package cl.skinspad.reporte.soporte;
@org.springframework.stereotype.Service @lombok.RequiredArgsConstructor public class ObjetivoInicializador {
 private final SoporteObjetivoRepository repo;
 @org.springframework.transaction.annotation.Transactional(propagation=org.springframework.transaction.annotation.Propagation.REQUIRES_NEW) public void crear(String key){if(!repo.existsById(key))repo.saveAndFlush(new SoporteObjetivo(key));}
}
