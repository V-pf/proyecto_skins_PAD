package cl.skinspad.reporte.soporte;
import org.springframework.cloud.openfeign.FeignClient;import org.springframework.web.bind.annotation.*;
@FeignClient(name="soporteUsuario",url="${USUARIO_URL:http://localhost:8087}") public interface SoporteUsuarioClient {
 record Usuario(String id,String rol,boolean suspendido){}
 @GetMapping("/api/internal/moderacion/usuarios/{id}") Usuario obtener(@PathVariable("id")String id);
 @PostMapping("/api/internal/moderacion/usuarios/{id}/suspender") void suspender(@PathVariable("id")String id,@RequestBody SoporteCatalogoClient.Orden orden);
}
