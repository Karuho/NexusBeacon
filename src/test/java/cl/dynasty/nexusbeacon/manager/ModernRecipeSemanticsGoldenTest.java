package cl.dynasty.nexusbeacon.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class ModernRecipeSemanticsGoldenTest {
    @Test void bundledPublicRecipeRemainsThePhaseSeventeenSourceOfTruth() throws Exception {
        YamlConfiguration beacon;
        try (InputStreamReader reader = new InputStreamReader(
                getClass().getResourceAsStream("/beacon.yml"), StandardCharsets.UTF_8)) {
            beacon = YamlConfiguration.loadConfiguration(reader);
        }

        assertTrue(beacon.getBoolean("recipe.enabled"));
        assertEquals(Arrays.asList("DDD", "DBD", "OOO"), beacon.getStringList("recipe.shape"));
        assertEquals("BEACON", beacon.getString("recipe.ingredients.B"));
        assertEquals("DIAMOND_BLOCK", beacon.getString("recipe.ingredients.D"));
        assertEquals("OBSIDIAN", beacon.getString("recipe.ingredients.O"));
        assertEquals("BEACON", beacon.getString("item.material"));
        assertEquals("%lang_item.display-name%", beacon.getString("item.display-name"));
        assertEquals(Arrays.asList("%lang_item.lore%"), beacon.getStringList("item.lore"));
    }
}
