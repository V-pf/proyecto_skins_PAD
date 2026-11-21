package cl.skinspad.steam.client;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.*;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;

@Component
public class SteamClient {
  private final RestClient steam = cliente("https://steamcommunity.com");

  public static RestClient cliente(String url) {
    var factory =
        new JdkClientHttpRequestFactory(
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
    factory.setReadTimeout(Duration.ofSeconds(20));
    return RestClient.builder().baseUrl(url).requestFactory(factory).build();
  }

  public List<ItemSteam> inventario(String steamId) {
    var items = new ArrayList<ItemSteam>();
    var cursores = new HashSet<String>();
    String cursor = "";
    try {
      do {
        final String siguiente = cursor;
        JsonNode pagina =
            steam
                .get()
                .uri(
                    b ->
                        b.path("/inventory/{id}/730/2")
                            .queryParam("l", "english")
                            .queryParam("count", 2000)
                            .queryParam("start_assetid", siguiente)
                            .build(steamId))
                .retrieve()
                .body(JsonNode.class);
        if (pagina == null || pagina.path("success").asInt() != 1)
          throw new IllegalStateException(
              "Steam no permitió consultar el inventario. Comprueba que sea público.");
        var descripciones = new HashMap<String, JsonNode>();
        for (JsonNode d : pagina.path("descriptions"))
          descripciones.put(d.path("classid").asText() + ":" + d.path("instanceid").asText(), d);
        for (JsonNode a : pagina.path("assets")) {
          JsonNode d =
              descripciones.get(a.path("classid").asText() + ":" + a.path("instanceid").asText());
          if (d == null)
            throw new IllegalStateException(
                "Steam devolvió un inventario incompleto. Reintenta la sincronización.");
          ItemSteam item = convertir(a, d);
          items.add(item);
        }
        if (!pagina.path("more_items").asBoolean()) break;
        cursor = pagina.path("last_assetid").asText();
        if (cursor.isBlank() || !cursores.add(cursor) || cursores.size() > 50)
          throw new IllegalStateException("No se pudo completar la paginación del inventario.");
      } while (true);
      return items;
    } catch (RestClientResponseException e) {
      if (e.getStatusCode().value() == 429)
        throw new IllegalStateException(
            "Steam limitó las consultas. Espera unos minutos antes de reintentar.");
      if (e.getStatusCode().value() == 403)
        throw new IllegalStateException(
            "Tu inventario de Steam es privado. Hazlo público y vuelve a sincronizar.");
      throw new IllegalStateException("Steam no está disponible. Reintenta más tarde.");
    } catch (ResourceAccessException e) {
      throw new IllegalStateException("Steam no está disponible. Reintenta más tarde.");
    }
  }

  static ItemSteam convertir(JsonNode asset, JsonNode description) {
    String assetId = asset.path("assetid").asText();
    String nombre = description.path("market_hash_name").asText(description.path("name").asText());
    var tags = new HashMap<String, String>();
    for (JsonNode tag : description.path("tags"))
      tags.put(tag.path("category").asText(), tag.path("internal_name").asText());
    String type = tags.getOrDefault("Type", "");
    String tipo =
        switch (type) {
          case "CSGO_Type_Rifle" -> "rifle";
          case "CSGO_Type_SniperRifle" -> "francotirador";
          case "CSGO_Type_Pistol" -> "pistola";
          case "CSGO_Type_SMG" -> "subfusil";
          case "CSGO_Type_Shotgun" -> "escopeta";
          case "CSGO_Type_Machinegun" -> "ametralladora";
          case "CSGO_Type_Knife" -> "cuchillo";
          case "Type_Hands" -> "guantes";
          case "CSGO_Tool_Sticker" -> "sticker";
          default -> null;
        };
    String grado =
        switch (tags.getOrDefault("Rarity", "")) {
          case "Rarity_Common_Weapon" -> "consumidor";
          case "Rarity_Uncommon_Weapon" -> "industrial";
          case "Rarity_Rare_Weapon" -> "militar";
          case "Rarity_Mythical_Weapon" -> "restringido";
          case "Rarity_Legendary_Weapon" -> "clasificado";
          case "Rarity_Ancient_Weapon" -> "encubierto";
          case "Rarity_Contraband" -> "contrabando";
          default -> "especial";
        };
    String desgaste =
        switch (tags.getOrDefault("Exterior", "")) {
          case "WearCategory0" -> "fn";
          case "WearCategory1" -> "mw";
          case "WearCategory2" -> "ft";
          case "WearCategory3" -> "ww";
          case "WearCategory4" -> "bs";
          default -> "na";
        };
    String icon = description.path("icon_url").asText();
    String imagen = icon.isBlank() ? null :
        "https://community.fastly.steamstatic.com/economy/image/" + icon;
    return new ItemSteam(assetId, nombre, tipo, grado, desgaste, nombre.contains("StatTrak"),
        imagen, description.path("tradable").asInt() == 1);
  }

  public record ItemSteam(String assetId, String nombre, String tipo, String grado, String desgaste,
      boolean stattrak, String imagen, boolean intercambiable) {}
}
