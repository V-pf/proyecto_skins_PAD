package cl.skinspad.pago.client;

import cl.skinspad.pago.dto.SaldoRequest;
import feign.FeignException;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Adaptador para traducir errores HTTP y exponer operaciones útiles para pagos. */
@Component
@RequiredArgsConstructor
public class UsuarioClient {

    private final UsuarioFeignClient cliente;

    public UsuarioRemoto buscar(String id) {
        try {
            UsuarioRemoto usuario = cliente.buscar(id);
            if (usuario == null || usuario.id() == null) {
                throw new EntityNotFoundException(
                    "El servicio de usuarios no devolvió al usuario solicitado"
                );
            }
            return usuario;
        } catch (FeignException.NotFound exception) {
            throw new EntityNotFoundException("Usuario no encontrado: " + id);
        }
    }

    public void recibo(String id,String ref,String detalle){cliente.recibo(id,new UsuarioFeignClient.Recibo(ref,detalle));}

    public void debitar(String id, BigDecimal monto, String referencia) {
        cliente.debitar(id, new SaldoRequest(monto, referencia));
    }

    public void abonar(String id, BigDecimal monto, String referencia) {
        cliente.abonar(id, new SaldoRequest(monto, referencia));
    }
}
