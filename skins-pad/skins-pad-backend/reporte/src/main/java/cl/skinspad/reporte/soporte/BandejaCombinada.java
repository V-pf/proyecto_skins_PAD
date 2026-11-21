package cl.skinspad.reporte.soporte;
import java.util.*;
import java.util.function.IntFunction;
import static cl.skinspad.reporte.soporte.SoporteDtos.*;

// Encuentra por búsqueda binaria cuántas filas aporta cada servicio antes de la página solicitada.
final class BandejaCombinada {
 private static final Comparator<Fila> ORDEN=Comparator.comparingInt((Fila f)->switch(f.prioridad()){
  case "URGENTE"->0; case "ALTA"->1; default->2;
 }).thenComparing(Fila::fecha).thenComparing(Fila::clase).thenComparing(Fila::id);
 private static class Fuente {
  final Map<Integer,Pagina> cache=new HashMap<>();
  final IntFunction<Pagina> cargar;
  final int tamano;
  Fuente(IntFunction<Pagina> cargar,int tamano){this.cargar=cargar;this.tamano=tamano;}
  Pagina pagina(int i){return cache.computeIfAbsent(i,cargar::apply);}
  Fila fila(int i){return pagina(i/tamano).filas().get(i%tamano);}
 }
 static Pagina obtener(int pagina,int tamano,IntFunction<Pagina> tickets,IntFunction<Pagina> reportes){
  if(pagina<0||pagina>10000||tamano<1||tamano>100)throw new IllegalArgumentException("Paginación inválida");
  var a=new Fuente(tickets,tamano);var b=new Fuente(reportes,tamano);
  long na=a.pagina(0).total(),nb=b.pagina(0).total();
  int salto=pagina*tamano;
  if(salto>=na+nb)return new Pagina(List.of(),na+nb,pagina,tamano);
  int bajo=(int)Math.max(0,salto-nb),alto=(int)Math.min(salto,na);
  while(bajo<alto){
   int i=(bajo+alto)/2,j=salto-i;
   if(j>0&&i<na&&ORDEN.compare(a.fila(i),b.fila(j-1))<0)bajo=i+1;
   else alto=i;
  }
  int i=bajo,j=salto-i;
  var filas=new ArrayList<Fila>();
  while(filas.size()<tamano&&(i<na||j<nb)){
   if(j>=nb||(i<na&&ORDEN.compare(a.fila(i),b.fila(j))<=0))filas.add(a.fila(i++));
   else filas.add(b.fila(j++));
  }
  return new Pagina(filas,na+nb,pagina,tamano);
 }
}
