package cl.skinspad.reporte.soporte;
import java.util.*;import java.math.*;import java.nio.charset.StandardCharsets;import static cl.skinspad.reporte.soporte.SoporteCatalogoClient.*;
public final class PrecioAtipico {
 private PrecioAtipico(){}
 public record Resultado(String estado,BigDecimal mediana,int comparables,String explicacion){}
 private static String nombre(String s){return s==null?"":java.text.Normalizer.normalize(s,java.text.Normalizer.Form.NFKC).strip().replaceAll("\\s+"," ").toLowerCase(Locale.ROOT);}
 private static boolean especial(Publicacion p){return "sticker".equals(p.tipo())||"especial".equals(p.grado())||"contrabando".equals(p.grado())||p.floatValor()==null||p.floatValor().compareTo(new BigDecimal("0.01"))<0||p.floatValor().compareTo(new BigDecimal("0.95"))>0;}
 public static String huella(Publicacion p){try{return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest((p.id()+"|"+nombre(p.nombre())+"|"+p.tipo()+"|"+p.desgaste()+"|"+p.stattrak()+"|"+p.floatValor()+"|"+p.precio()+"|"+p.grado()).getBytes(StandardCharsets.UTF_8)));}catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
 public static Resultado calcular(Publicacion p,List<Publicacion> datos,int minimo,BigDecimal superior,BigDecimal inferior){
  if(!"ACTIVO".equals(p.estado()))return new Resultado("INACTIVA",null,0,"Publicación no activa");
  if(especial(p))return new Resultado("SIN_DATOS",null,0,"Sin datos suficientes: pieza especial, sticker o float extremo requiere revisión manual");
  var precios=datos.stream().filter(x->!x.id().equals(p.id())&&"ACTIVO".equals(x.estado())&&!x.reservada()&&!especial(x)&&nombre(p.nombre()).equals(nombre(x.nombre()))&&Objects.equals(p.tipo(),x.tipo())&&Objects.equals(p.desgaste(),x.desgaste())&&Objects.equals(p.stattrak(),x.stattrak())&&x.precio()!=null&&x.precio().signum()>0).map(Publicacion::precio).sorted().toList();
  if(precios.size()<Math.max(3,minimo))return new Resultado("SIN_DATOS",null,precios.size(),"Sin datos suficientes: se requieren al menos "+Math.max(3,minimo)+" comparables distintos de esta publicación");
  int mitad=precios.size()/2;BigDecimal mediana=precios.size()%2==0?precios.get(mitad-1).add(precios.get(mitad)).divide(BigDecimal.valueOf(2)):precios.get(mitad);
  boolean atipico=p.precio().compareTo(mediana.multiply(superior))>0||p.precio().compareTo(mediana.multiply(inferior))<0;
  return new Resultado(atipico?"PENDIENTE":"NORMAL",mediana,precios.size(),atipico?"Precio fuera del intervalo de referencia; no demuestra fraude":"Precio dentro del intervalo de referencia");
 }
}
