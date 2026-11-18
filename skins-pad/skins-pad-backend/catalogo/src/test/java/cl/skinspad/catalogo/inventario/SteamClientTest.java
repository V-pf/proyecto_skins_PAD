package cl.skinspad.catalogo.inventario;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class SteamClientTest {
  @Test
  void importaLaIdentidadDelAssetYLosAtributosDeSteam() throws Exception {
    var mapper = new ObjectMapper();
    var asset = mapper.readTree("{\"assetid\":\"123\"}");
    var description =
        mapper.readTree(
            "{\"market_hash_name\":\"StatTrak™ AK-47 | Redline"
                + " (Field-Tested)\",\"tradable\":1,\"tags\":[{\"category\":\"Type\",\"internal_name\":\"CSGO_Type_Rifle\"},{\"category\":\"Exterior\",\"internal_name\":\"WearCategory2\"},{\"category\":\"Rarity\",\"internal_name\":\"Rarity_Legendary_Weapon\"}]}");
    var item = SteamClient.convertir(asset, description);
    assertEquals("123", item.getAssetId());
    assertEquals("rifle", item.getTipo());
    assertEquals("ft", item.getDesgaste());
    assertEquals("clasificado", item.getGrado());
    assertTrue(item.isStattrak());
    assertTrue(item.isIntercambiable());
  }
}
