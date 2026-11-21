package cl.skinspad.reporte.soporte;
import java.util.*;import java.math.*;import org.junit.jupiter.api.Test;import static org.junit.jupiter.api.Assertions.*;
class PrecioAtipicoTest {
 SoporteCatalogoClient.Publicacion skin(long id,long p){return new SoporteCatalogoClient.Publicacion(id,"AK-47 | Redline","s","rifle","ft",false,new BigDecimal("0.2"),"clasificado",BigDecimal.valueOf(p),"ACTIVO",false);}
 @Test void medianaUmbralesYDatosInsuficientes(){var datos=List.of(skin(2,100),skin(3,100),skin(4,100),skin(5,100),skin(6,100));for(long valor:new long[]{301,29})assertEquals("PENDIENTE",PrecioAtipico.calcular(skin(1,valor),datos,5,new BigDecimal("3"),new BigDecimal("0.3")).estado());for(long valor:new long[]{300,30})assertEquals("NORMAL",PrecioAtipico.calcular(skin(1,valor),datos,5,new BigDecimal("3"),new BigDecimal("0.3")).estado());assertEquals("SIN_DATOS",PrecioAtipico.calcular(skin(1,1000),List.of(skin(1,1000)),5,new BigDecimal("3"),new BigDecimal("0.3")).estado());}
 @Test void normalizaNombreSinMezclarStatTrakNiDesgaste(){var p=skin(1,1000);var otro=new SoporteCatalogoClient.Publicacion(2L,"  ak-47  |  redline  ","s","rifle","ft",false,new BigDecimal("0.2"),"clasificado",BigDecimal.valueOf(100),"ACTIVO",false);assertEquals(1,PrecioAtipico.calcular(p,List.of(otro),5,new BigDecimal("3"),new BigDecimal("0.3")).comparables());}
}
