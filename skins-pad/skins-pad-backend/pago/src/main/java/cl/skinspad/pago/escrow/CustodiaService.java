package cl.skinspad.pago.escrow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
@Service @RequiredArgsConstructor
public class CustodiaService {
 private final CustodiaTransactions tx;
 public void pago(Long id){try{tx.procesarPago(id);}catch(feign.FeignException e){tx.falloPago(id,e.status()==409);if(e.status()==409)tx.procesarPago(id);}catch(IllegalArgumentException e){tx.falloPago(id,true);tx.procesarPago(id);}}
 public void accion(Long id,MaquinaOperacion.Accion a,String actor,String rol,String motivo,boolean sistema){tx.solicitar(id,a,actor,rol,motivo,sistema);procesar(id);}
 public void procesar(Long id){try{tx.procesar(id);}catch(RuntimeException e){tx.fallo(id);}}
}
