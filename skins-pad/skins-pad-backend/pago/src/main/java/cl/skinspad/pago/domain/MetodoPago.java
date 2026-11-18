package cl.skinspad.pago.domain;
// Formas de pago reconocidas al iniciar la compra.
public enum MetodoPago {
    TRANSFERENCIA, TARJETA, MERCADO_PAGO, SALDO;

    public static void validarCompra(MetodoPago metodo) {
        if (metodo != TARJETA && metodo != SALDO)
            throw new IllegalArgumentException("Selecciona tarjeta o saldo de la billetera");
    }
}
