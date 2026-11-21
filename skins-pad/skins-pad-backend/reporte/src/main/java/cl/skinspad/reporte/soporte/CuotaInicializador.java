package cl.skinspad.reporte.soporte;
@org.springframework.stereotype.Service @lombok.RequiredArgsConstructor public class CuotaInicializador {
 private final SoporteCuotaRepository cuotas;
 @org.springframework.transaction.annotation.Transactional(propagation=org.springframework.transaction.annotation.Propagation.REQUIRES_NEW) public void crear(String id){if(!cuotas.existsById(id))cuotas.saveAndFlush(new SoporteCuota(id));}
}
