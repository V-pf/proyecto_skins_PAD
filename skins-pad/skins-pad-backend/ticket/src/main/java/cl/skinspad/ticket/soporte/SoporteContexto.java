package cl.skinspad.ticket.soporte;
import lombok.RequiredArgsConstructor;import org.springframework.stereotype.Service;import static cl.skinspad.ticket.soporte.EstadosSoporte.*;
@Service @RequiredArgsConstructor public class SoporteContexto {
 private final SoporteCatalogoClient catalogo;private final SoporteUsuarioClient usuarios;private final PagoClient operaciones;
 public PagoClient.Operacion operacion(Long id){return operaciones.obtener(id);}
 public void participante(PagoClient.Operacion o,SoporteIdentidad.Actor a){if(!a.admin()&&!o.compradorId().equals(a.id())&&!o.vendedorId().equals(a.id()))throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN,"Solo puedes referenciar operaciones propias");}
 public void referencias(Long op,Long arma,SoporteIdentidad.Actor a){if(op!=null){var o=operacion(op);participante(o,a);if(arma!=null&&!arma.equals(o.productoId()))throw new IllegalArgumentException("La publicación no pertenece a esta operación");}if(arma!=null)catalogo.obtener(arma);}
 public void adminAsignable(String id){if(id==null)return;var a=usuarios.obtener(ValidacionSoporte.id(id));if(!"admin".equals(a.rol())||a.suspendido())throw new IllegalArgumentException("El asignado debe ser un administrador activo");}

}