package cl.skinspad.reporte.soporte;
@jakarta.persistence.Entity @jakarta.persistence.Table(name="soporte_objetivo") @lombok.Getter @lombok.NoArgsConstructor public class SoporteObjetivo {
 @jakarta.persistence.Id @jakarta.persistence.Column(length=120) private String clave;
 public SoporteObjetivo(String c){clave=c;}
}
