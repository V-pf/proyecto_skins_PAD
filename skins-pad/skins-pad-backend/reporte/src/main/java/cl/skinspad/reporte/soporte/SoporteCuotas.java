package cl.skinspad.reporte.soporte;
import java.time.*;import org.springframework.stereotype.Service;import lombok.RequiredArgsConstructor;
@Service @RequiredArgsConstructor public class SoporteCuotas {
 private final CuotaInicializador inicializador;private final SoporteCuotaRepository cuotas;
 @org.springframework.beans.factory.annotation.Value("${soporte.reportes-dia:10}") private int limite;
 public void preparar(String id){try{inicializador.crear(id);}catch(org.springframework.dao.DataIntegrityViolationException e){if(!cuotas.existsById(id))throw e;}}
 public void consumir(String id){
  var c=cuotas.bloquear(id).orElseThrow();
  var hoy=LocalDate.now(ZoneId.of("America/Santiago"));
  if(!hoy.equals(c.getDia())){c.setDia(hoy);c.setReportes(0);}
  if(c.getReportes()>=limite)throw new org.springframework.web.server.ResponseStatusException(
      org.springframework.http.HttpStatus.TOO_MANY_REQUESTS,"Alcanzaste el límite diario de "+limite+" reportes. Intenta mañana (hora de Santiago)");
  c.setReportes(c.getReportes()+1);
 }
}
