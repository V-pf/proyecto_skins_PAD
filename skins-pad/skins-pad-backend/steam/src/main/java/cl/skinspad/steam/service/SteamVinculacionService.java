package cl.skinspad.steam.service;

import cl.skinspad.steam.domain.SteamEstado;
import cl.skinspad.steam.domain.VinculoSteam;
import cl.skinspad.steam.repository.SteamEstadoRepository;
import cl.skinspad.steam.repository.VinculoRepository;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SteamVinculacionService {
  private final VinculoRepository vinculos;
  private final SteamEstadoRepository estados;
  private final SteamOpenId steam;

  @Transactional(readOnly = true)
  public String steamId(String usuarioId) {
    return vinculos.findById(usuarioId).map(VinculoSteam::getSteamId).orElse(null);
  }

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
    return steam.iniciar(state);
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
    String steamId = steam.confirmar(state, params);
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
    vinculos.saveAndFlush(vinculo);
    estados.delete(estado);
    return vinculo.getUsuarioId();
  }
}
