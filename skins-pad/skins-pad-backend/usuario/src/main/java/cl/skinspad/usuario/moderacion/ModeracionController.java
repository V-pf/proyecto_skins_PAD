package cl.skinspad.usuario.moderacion;
import org.springframework.web.bind.annotation.*;import lombok.RequiredArgsConstructor;
@RestController @RequestMapping("/api/internal/moderacion") @RequiredArgsConstructor
public class ModeracionController {
 private final cl.skinspad.usuario.service.UsuarioService service;private final cl.skinspad.usuario.repository.UsuarioRepository usuarios;private final AccionReporteRepository acciones;
 public record UsuarioVista(String id,String rol,boolean suspendido){}
 public record Orden(String referencia,String actor,String motivo){}
 @GetMapping("/usuarios/{id}") public UsuarioVista obtener(@PathVariable String id){var u=service.buscar(id);return new UsuarioVista(u.getId(),u.getRol().name(),u.isSuspendido());}
 @PostMapping("/usuarios/{id}/suspender") @org.springframework.transaction.annotation.Transactional public void suspender(@PathVariable String id,@RequestBody Orden r){validar(r);var admin=service.buscar(r.actor());if(!admin.getRol().name().equals("admin")||admin.isSuspendido())throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN);
  var u=usuarios.bloquear(id).orElseThrow();var previa=acciones.findById(r.referencia());if(previa.isPresent()){if(!previa.get().getObjetivo().equals(id)||!previa.get().getAdminId().equals(r.actor())||!previa.get().getMotivo().equals(r.motivo()))throw new IllegalStateException("Referencia usada para otra acción");return;}
  if("admin".equals(u.getRol().name())||id.equals(r.actor()))throw new IllegalStateException("No se puede suspender esta cuenta desde un reporte");
  service.actualizarEstado(id,new cl.skinspad.usuario.dto.EstadoUsuarioRequest(true,null,null,r.motivo()));acciones.save(new AccionReporte(r.referencia(),id,r.actor(),r.motivo(),"SUSPENDER"));
 }
 private void validar(Orden r){if(r.referencia()==null||!r.referencia().matches("REP-[0-9]+-[a-zA-Z0-9-]+")||r.referencia().length()>80||r.actor()==null||r.motivo()==null||r.motivo().isBlank()||r.motivo().length()>1000||r.motivo().contains("<")||r.motivo().contains(">"))throw new IllegalArgumentException("Orden de moderación inválida");}
}
