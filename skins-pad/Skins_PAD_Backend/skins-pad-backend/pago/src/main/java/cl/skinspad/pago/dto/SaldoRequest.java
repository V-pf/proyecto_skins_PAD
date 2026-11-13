package cl.skinspad.pago.dto;

import java.math.BigDecimal;
public record SaldoRequest(BigDecimal monto, String referencia) {
}
