package cl.skinspad.catalogo.inventario;

import java.security.SecureRandom;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class SteamVinculacionService {
  private final VinculoRepository vinculos;
  private final SteamEstadoRepository estados;

  @Value("${STEAM_CALLBACK_URL:http://localhost:8090/api/steam/callback}")
  private String callback;

  @Transactional
  public String iniciar(String usuarioId) {
    estados.deleteByVenceBefore(Instant.now());
    byte[] bytes = new byte[32];
    new SecureRandom().nextBytes(bytes);
    String state = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    var estado = new SteamEstado();
    estado.setEstado(state);
    estado.setUsuarioId(usuarioId);
    estado.setVence(Instant.now().plusSeconds(600));
    estados.save(estado);
    String retorno =
        UriComponentsBuilder.fromUriString(callback)
            .queryParam("state", state)
            .build()
            .toUriString();
    return UriComponentsBuilder.fromUriString("https://steamcommunity.com/openid/login")
        .queryParam("openid.ns", "http://specs.openid.net/auth/2.0")
        .queryParam("openid.mode", "checkid_setup")
        .queryParam("openid.return_to", retorno)
        .queryParam("openid.realm", java.net.URI.create(callback).resolve("/").toString())
        .queryParam("openid.identity", "http://specs.openid.net/auth/2.0/identifier_select")
        .queryParam("openid.claimed_id", "http://specs.openid.net/auth/2.0/identifier_select")
        .build()
        .encode()
        .toUriString();
  }

  @Transactional
  public String confirmar(Map<String, String> params) {
    String state = params.getOrDefault("state", "");
    var estado =
        estados
            .bloquear(state)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "La vinculación no es válida o ya fue utilizada."));
    if (estado.getVence().isBefore(Instant.now()))
      throw new IllegalArgumentException("La vinculación expiró. Iníciala de nuevo.");
    String claimed = params.getOrDefault("openid.claimed_id", "");
    String retorno =
        UriComponentsBuilder.fromUriString(callback)
            .queryParam("state", state)
            .build()
            .toUriString();
    Set<String> firmados =
        new HashSet<>(Arrays.asList(params.getOrDefault("openid.signed", "").split(",")));
    if (!"id_res".equals(params.get("openid.mode"))
        || !"http://specs.openid.net/auth/2.0".equals(params.get("openid.ns"))
        || !"https://steamcommunity.com/openid/login".equals(params.get("openid.op_endpoint"))
        || !claimed.matches("https://steamcommunity.com/openid/id/[0-9]{17}")
        || !claimed.equals(params.get("openid.identity"))
        || !retorno.equals(params.get("openid.return_to"))
        || !firmados.containsAll(
            Set.of(
                "op_endpoint",
                "claimed_id",
                "identity",
                "return_to",
                "response_nonce",
                "assoc_handle")))
      throw new IllegalArgumentException("La respuesta de Steam no es válida.");
    String nonce = params.getOrDefault("openid.response_nonce", "");
    try {
      Instant fecha = Instant.parse(nonce.substring(0, 20));
      if (fecha.isBefore(Instant.now().minusSeconds(600))
          || fecha.isAfter(Instant.now().plusSeconds(60))) throw new IllegalArgumentException();
    } catch (RuntimeException e) {
      throw new IllegalArgumentException("La respuesta de Steam expiró.");
    }
    var form = new LinkedMultiValueMap<String, String>();
    params.forEach(
        (k, v) -> {
          if (k.startsWith("openid.")) form.add(k, v);
        });
    form.set("openid.mode", "check_authentication");
    String respuesta =
        SteamClient.cliente("https://steamcommunity.com")
            .post()
            .uri("/openid/login")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(form)
            .retrieve()
            .body(String.class);
    if (respuesta == null || respuesta.lines().noneMatch("is_valid:true"::equals))
      throw new IllegalArgumentException("Steam no pudo verificar la identidad.");
    String steamId = claimed.substring(claimed.lastIndexOf('/') + 1);
    var vinculo =
        vinculos
            .bloquear(estado.getUsuarioId())
            .orElseGet(
                () -> {
                  var v = new VinculoSteam();
                  v.setUsuarioId(estado.getUsuarioId());
                  return v;
                });
    if (vinculo.getSteamId() != null && !steamId.equals(vinculo.getSteamId()))
      throw new IllegalStateException("Esta cuenta ya tiene otro perfil Steam vinculado.");
    if (!steamId.equals(vinculo.getSteamId()) && vinculos.existsBySteamId(steamId))
      throw new IllegalStateException("Este perfil Steam ya está vinculado a otra cuenta.");
    vinculo.setSteamId(steamId);
    vinculo.setDemo(false);
    vinculos.saveAndFlush(vinculo);
    estados.delete(estado);
    return vinculo.getUsuarioId();
  }
}
