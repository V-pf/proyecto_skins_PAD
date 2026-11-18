package cl.skinspad.catalogo.inventario;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "INVENTARIO_DEMO", havingValue = "true")
public class InventarioDemoSeed implements CommandLineRunner {
  private final InventarioService service;

  @Value("${USUARIO_URL:http://localhost:8087}")
  private String usuarios;

  public void run(String... args) {
    try {
      var cuentas =
          SteamClient.cliente(usuarios)
              .get()
              .uri("/api/usuarios/publicos")
              .retrieve()
              .body(InventarioAuth.Cuenta[].class);
      if (cuentas != null)
        for (var cuenta : cuentas)
          if ("cliente".equals(cuenta.rol())) service.prepararDemo(cuenta.id());
    } catch (org.springframework.web.client.RestClientException e) {
      org.slf4j.LoggerFactory.getLogger(getClass())
          .warn(
              "Los inventarios de prueba se crearán al abrir Mi inventario porque usuario no está"
                  + " disponible.");
    }
  }
}
