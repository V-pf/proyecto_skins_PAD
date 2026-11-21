package cl.skinspad.reporte.soporte;
import java.util.*;import lombok.RequiredArgsConstructor;import org.springframework.stereotype.Service;import static cl.skinspad.reporte.soporte.SoporteDtos.*;
@Service @RequiredArgsConstructor public class SoporteRegistro {
 private final SoporteEventoRepository eventos;private final SoporteNotificacionRepository avisos;
 public void evento(String e,Long id,String actor,String accion,Object antes,Object despues,String motivo){eventos.save(new SoporteEvento(e,id,actor,accion,antes,despues,motivo));}
 public void avisar(String clave,String para,String texto,String enlace){if(!avisos.existsByClave(clave))avisos.save(new SoporteNotificacion(clave,para,texto,enlace));}
 public List<EventoVista> historial(String e,Long id){return eventos.findByEntidadAndEntidadIdOrderById(e,id).stream().map(v->new EventoVista(v.getId(),v.getAutor(),v.getFecha(),v.getAccion(),v.getAnterior(),v.getNuevo(),v.getMotivo())).toList();}
 public List<Aviso> avisos(SoporteIdentidad.Actor a){return avisos.findTop50ByDestinatarioOrderByIdDesc(a.admin()?"ADMIN":a.id()).stream().map(n->new Aviso(n.getId(),n.getTexto(),n.getEnlace(),n.getFecha())).toList();}
}
