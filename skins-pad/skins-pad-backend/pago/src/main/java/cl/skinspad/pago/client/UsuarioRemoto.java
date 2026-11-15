package cl.skinspad.pago.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UsuarioRemoto(String id, BigDecimal saldo, Boolean suspendido) {
}
