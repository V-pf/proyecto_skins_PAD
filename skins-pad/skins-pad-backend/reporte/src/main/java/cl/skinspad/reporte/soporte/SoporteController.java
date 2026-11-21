package cl.skinspad.reporte.soporte;
import java.time.*;import java.util.*;import lombok.RequiredArgsConstructor;import org.springframework.web.bind.annotation.*;import org.springframework.http.*;import org.springframework.format.annotation.DateTimeFormat;import com.fasterxml.jackson.databind.JsonNode;import static cl.skinspad.reporte.soporte.SoporteDtos.*;
@RestController @RequestMapping("/api") @RequiredArgsConstructor public class SoporteController {
 private final TicketConsultas consulta; private final SoporteIdentidad identidad;private final SoporteRegistro registro;private final ReporteService reportes;private final SoporteAdjuntoRepository adjuntos;private final BandejaRepository bandeja;private final SoporteStats stats;private final PrecioService precios;private final SoporteJson json;
 @GetMapping("/soporte/notificaciones") public List<Aviso> notificaciones(@RequestHeader(value="Authorization",required=false)String auth){var actor=identidad.validar(auth,false);
  return java.util.stream.Stream.concat(registro.avisos(actor).stream(), consulta.avisos(auth).stream())
   .sorted(Comparator.comparing(Aviso::fecha).reversed()).limit(50).toList();}
 private void comprobar(String tipo,Long id,SoporteIdentidad.Actor a){switch(tipo){case "REPORTE"->reportes.acceso(id,a);case "ADJUNTO"->{var f=adjuntos.findById(id).orElseThrow(()->new jakarta.persistence.EntityNotFoundException("Adjunto no encontrado"));comprobar(f.getEntidad(),f.getEntidadId(),a);}default->throw new IllegalArgumentException("Tipo inválido");}}
 @GetMapping("/soporte/acceso") public Map<String,Boolean> acceso(@RequestHeader(value="Authorization",required=false)String auth,@RequestParam String tipo,@RequestParam Long id){comprobar(tipo,id,identidad.validar(auth,false));return Map.of("permitido",true);}
 @GetMapping("/reportes/adjuntos/{id}") public ResponseEntity<byte[]> adjunto(@PathVariable Long id,@RequestHeader(value="Authorization",required=false)String auth){var a=identidad.validar(auth,false);comprobar("ADJUNTO",id,a);var f=adjuntos.findById(id).orElseThrow();return ResponseEntity.ok().contentType(MediaType.parseMediaType(f.getMime())).header("Content-Disposition",ContentDisposition.attachment().filename("evidencia-"+id+(f.getMime().equals("image/png")?".png":".jpg")).build().toString()).header("X-Content-Type-Options","nosniff").header("Content-Security-Policy","default-src 'none'; sandbox").header("Cache-Control","no-store").body(f.getDatos());}
 @GetMapping({"/admin/reportes","/admin/soporte/bandeja"}) public Pagina bandeja(@RequestHeader(value="Authorization",required=false)String auth,@RequestParam(required=false)String clase,@RequestParam(required=false)String estado,@RequestParam(required=false)String prioridad,@RequestParam(required=false)String tipo,@RequestParam(required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate desde,@RequestParam(required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate hasta,@RequestParam(defaultValue="0")int pagina,@RequestParam(defaultValue="20")int tamano,jakarta.servlet.http.HttpServletRequest request){identidad.validar(auth,true);String ruta=request.getRequestURI();if(ruta.endsWith("/reportes")||"REPORTE".equals(clase))
   return bandeja.buscar("REPORTE",estado,prioridad,tipo,desde,hasta,pagina,tamano);
  var filtros=new HashMap<String,String>();
  for(String nombre:List.of("estado","prioridad","tipo","desde","hasta","tamano"))
   if(request.getParameter(nombre)!=null)filtros.put(nombre,request.getParameter(nombre));
  filtros.put("tamano",String.valueOf(tamano));
  java.util.function.IntFunction<Pagina> tickets=i->{var q=new HashMap<>(filtros);q.put("pagina",String.valueOf(i));return consulta.bandeja(auth,q);};
  if("TICKET".equals(clase))return tickets.apply(pagina);
  if(clase!=null&&!clase.isBlank())return new Pagina(List.of(),0,pagina,tamano);
  return BandejaCombinada.obtener(pagina,tamano,tickets,
    i->bandeja.buscar("REPORTE",estado,prioridad,tipo,desde,hasta,i,tamano));}
 @GetMapping("/admin/soporte/stats") public SoporteStats.Resumen stats(@RequestHeader(value="Authorization",required=false)String auth){identidad.validar(auth,true);var r=stats.obtener(); var t=consulta.stats(auth);
  return new SoporteStats.Resumen(t.pendientes()+r.pendientes(),t.tickets(),r.reportes(),t.horasTicket(),
   r.horasReporte(),r.motivos(),r.preciosPendientes(),r.porcentajeRechazados(),r.denunciados());}
 @GetMapping("/admin/soporte/precios") public List<AlertaVista> precios(@RequestHeader(value="Authorization",required=false)String auth){identidad.validar(auth,true);return precios.pendientes();}
 @PostMapping("/admin/soporte/precios/analizar") public List<AlertaVista> analizar(@RequestHeader(value="Authorization",required=false)String auth){identidad.validar(auth,true);precios.analizar();return precios.pendientes();}
 @PostMapping("/admin/soporte/precios/{id}/validar") public AlertaVista validar(@PathVariable Long id,@RequestHeader(value="Authorization",required=false)String auth,@RequestBody JsonNode b){var a=identidad.validar(auth,true);return precios.validar(id,json.leer(b,ValidarPrecio.class),a.id());}
}
