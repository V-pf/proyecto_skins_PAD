package cl.skinspad.catalogo.moderacion;
import org.springframework.web.bind.annotation.*;import lombok.RequiredArgsConstructor;import java.math.BigDecimal;import java.util.List;
@RestController @RequestMapping("/api/internal/moderacion") @RequiredArgsConstructor
public class ModeracionController {
 private final cl.skinspad.catalogo.service.CatalogoService service;private final cl.skinspad.catalogo.repository.ProductoRepository productos;private final AccionReporteRepository acciones;private final cl.skinspad.catalogo.config.InternalApiKey key;
 @org.springframework.beans.factory.annotation.Value("${USUARIO_URL:http://localhost:8087}") private String usuarioUrl;
 public record Publicacion(Long id,String nombre,String vendedorId,String tipo,String desgaste,Boolean stattrak,BigDecimal floatValor,String grado,BigDecimal precio,String estado,boolean reservada){}
 public record Orden(String referencia,String actor,String motivo){}
 private Publicacion vista(cl.skinspad.catalogo.domain.Producto p){return new Publicacion(p.getId(),p.getNombre(),p.getVendedorId(),p.getTipo(),p.getDesgaste(),p.getStattrak(),p.getFloatValor(),p.getGrado(),p.getPrecio(),p.getEstado().name(),p.getReservaPago()!=null);}
 @GetMapping("/publicaciones") public List<Publicacion> listar(){return productos.findAll().stream().map(this::vista).toList();}
 @GetMapping("/publicaciones/{id}") public Publicacion obtener(@PathVariable Long id){return vista(service.buscarPorId(id));}
 @PostMapping("/publicaciones/{id}/despublicar") @org.springframework.transaction.annotation.Transactional public void despublicar(@PathVariable Long id,@RequestBody Orden r){
  if(r.referencia()==null||!r.referencia().matches("REP-[0-9]+-[a-zA-Z0-9-]+")||r.referencia().length()>80||r.actor()==null||r.motivo()==null||r.motivo().isBlank()||r.motivo().length()>500||r.motivo().contains("<")||r.motivo().contains(">"))throw new IllegalArgumentException("Orden de moderación inválida");var admin=org.springframework.web.client.RestClient.builder().baseUrl(usuarioUrl).requestFactory(tiempos()).defaultHeader("X-Skins-Internal",key.value()).build().get().uri("/api/internal/moderacion/usuarios/{id}",r.actor()).retrieve().body(java.util.Map.class);if(admin==null||!"admin".equals(admin.get("rol"))||Boolean.TRUE.equals(admin.get("suspendido")))throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN);
  productos.bloquear(id).orElseThrow();var previa=acciones.findById(r.referencia());if(previa.isPresent()){if(!previa.get().getObjetivo().equals(id.toString())||!previa.get().getAdminId().equals(r.actor())||!previa.get().getMotivo().equals(r.motivo()))throw new IllegalStateException("Referencia usada para otra acción");return;}
  service.eliminarProducto(id,r.motivo(),true);acciones.save(new AccionReporte(r.referencia(),id.toString(),r.actor(),r.motivo(),"DESPUBLICAR"));
 }
 private org.springframework.http.client.SimpleClientHttpRequestFactory tiempos(){var f=new org.springframework.http.client.SimpleClientHttpRequestFactory();f.setConnectTimeout(3000);f.setReadTimeout(5000);return f;}
}
