package cl.skinspad.pago.client;

import cl.skinspad.pago.dto.SaldoRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/** Cliente HTTP hacia el microservicio propietario de las cuentas y saldos. */
@FeignClient(
    name = "skins-pad-usuario",
    url = "${services.usuario-url:http://localhost:8082}"
)
public interface UsuarioFeignClient {

    record Recibo(String referencia,String detalle){}
    @PostMapping("/api/usuarios/{id}/saldo/recibo") void recibo(@PathVariable("id") String id,@RequestBody Recibo r);

    @GetMapping("/api/usuarios/{id}")
    UsuarioRemoto buscar(@PathVariable("id") String id);

    @PostMapping("/api/usuarios/{id}/saldo/debitar")
    void debitar(@PathVariable("id") String id, @RequestBody SaldoRequest request);

    @PostMapping("/api/usuarios/{id}/saldo/abonar")
    void abonar(@PathVariable("id") String id, @RequestBody SaldoRequest request);
}
