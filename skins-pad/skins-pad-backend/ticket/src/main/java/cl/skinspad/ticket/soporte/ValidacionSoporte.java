package cl.skinspad.ticket.soporte;
import java.util.*;import java.text.Normalizer;import java.net.URI;
public final class ValidacionSoporte {
 private ValidacionSoporte(){}
 public static String texto(String s,int max){String v=s==null?"":Normalizer.normalize(s,Normalizer.Form.NFKC).strip();if(v.isBlank()||v.length()>max)throw new IllegalArgumentException("Texto obligatorio, máximo "+max+" caracteres");if(v.contains("<")||v.contains(">")||v.codePoints().anyMatch(c->Character.isISOControl(c)&&c!=10&&c!=13&&c!=9))throw new IllegalArgumentException("Usa texto simple sin HTML ni caracteres de control");return v;}
 public static String id(String s){if(s==null||!s.matches("[a-zA-Z0-9_-]{1,80}"))throw new IllegalArgumentException("Identificador inválido");return s;}
 public static Long numero(String s){try{long n=Long.parseLong(s);if(n<=0)throw new Exception();return n;}catch(Exception e){throw new IllegalArgumentException("Identificador numérico inválido");}}
 public static List<String> urls(List<String> urls){if(urls==null)return List.of();if(urls.size()>3)throw new IllegalArgumentException("Máximo 3 enlaces de evidencia");return urls.stream().map(s->{String v=texto(s,500);try{URI u=URI.create(v);if(!"https".equalsIgnoreCase(u.getScheme())||u.getHost()==null||u.getUserInfo()!=null)throw new Exception();}catch(Exception e){throw new IllegalArgumentException("La evidencia debe usar una URL HTTPS sin credenciales");}return v;}).toList();}
 public static void version(Long expected,long actual){if(expected==null||expected!=actual)throw new IllegalStateException("El caso cambió, recarga antes de continuar");}
}
