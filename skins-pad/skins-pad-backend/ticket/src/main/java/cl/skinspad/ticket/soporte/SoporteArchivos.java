package cl.skinspad.ticket.soporte;
import java.util.*;import java.io.*;import javax.imageio.ImageIO;import lombok.RequiredArgsConstructor;import org.springframework.stereotype.Service;import static cl.skinspad.ticket.soporte.SoporteDtos.*;
@Service @RequiredArgsConstructor public class SoporteArchivos {
 private final SoporteAdjuntoRepository repo;
 public static byte[] validar(Archivo a){
  if(a==null||a.mime()==null||!Set.of("image/png","image/jpeg").contains(a.mime())||a.base64()==null||a.base64().length()>700000)throw new IllegalArgumentException("Solo PNG/JPEG de hasta 512 KiB");
  ValidacionSoporte.texto(a.nombre(),100);
  try{byte[] bytes=Base64.getDecoder().decode(a.base64());if(bytes.length==0||bytes.length>524288)throw new IllegalArgumentException();
   try(var in=ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))){var readers=ImageIO.getImageReaders(in);if(!readers.hasNext())throw new IllegalArgumentException();var reader=readers.next();try{reader.setInput(in);int w=reader.getWidth(0),h=reader.getHeight(0);String fmt=reader.getFormatName();if(w>4096||h>4096||(long)w*h>10000000||!(a.mime().equals("image/png")?fmt.equalsIgnoreCase("png"):fmt.equalsIgnoreCase("jpeg")))throw new IllegalArgumentException();var img=reader.read(0);var out=new ByteArrayOutputStream();ImageIO.write(img,a.mime().equals("image/png")?"png":"jpeg",out);if(out.size()>524288)throw new IllegalArgumentException();return out.toByteArray();}finally{reader.dispose();}}
  }catch(Exception e){throw new IllegalArgumentException("Imagen inválida: PNG/JPEG real, máximo 512 KiB, 4096 px por lado y 10 megapíxeles");}
 }
 public void guardar(String entidad,Long id,Long mensaje,List<Archivo> archivos){if(archivos==null)return;if(archivos.size()>3)throw new IllegalArgumentException("Máximo 3 imágenes por mensaje o reporte");for(var a:archivos){var p=new SoporteAdjunto();p.setDatos(validar(a));p.setNombre(a.nombre().replaceAll("[^\\p{L}\\p{N} ._-]","_"));p.setMime(a.mime());p.setEntidad(entidad);p.setEntidadId(id);p.setMensajeId(mensaje);repo.save(p);}}
 public List<AdjuntoVista> listar(String e,Long id){return repo.findByEntidadAndEntidadId(e,id).stream().map(a->new AdjuntoVista(a.getId(),a.getNombre(),a.getMime(),a.getMensajeId())).toList();}
}
