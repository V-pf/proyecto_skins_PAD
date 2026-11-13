package cl.skinspad.carrito.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductoRemoto(Long id, String nombre, BigDecimal precio, String estado, String vendedorId) {
    public ProductoRemoto(Long id,String nombre,BigDecimal precio,String estado){this(id,nombre,precio,estado,null);}
}
