package cl.dynasty.nexusbeacon.beam;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class ModernBeamHeightTest {

    @Test
    void worldMaxUsesRuntimeMaximumIncludingFromNegativeY() {
        assertEquals(319, ModernBeamHeight.height(ModernBeamHeight.WORLD_MAX, 96, 0, 320));
        assertEquals(377, ModernBeamHeight.height(ModernBeamHeight.WORLD_MAX, 96, -58, 320));
    }

    @Test
    void fixedAndLegacyMissingModeRespectConfiguredHeight() {
        YamlConfiguration legacy = new YamlConfiguration();
        legacy.set("visual-beam.height", 73);
        assertEquals(ModernBeamHeight.FIXED, ModernBeamHeight.mode(legacy));
        assertEquals(73, ModernBeamHeight.height(ModernBeamHeight.mode(legacy), 73, -58, 320));
    }

    @Test
    void bundledDefaultIsWorldMax() throws Exception {
        try (var input = getClass().getResourceAsStream("/beacon.yml")) {
            var config = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(input, StandardCharsets.UTF_8));
            assertEquals(ModernBeamHeight.WORLD_MAX, ModernBeamHeight.mode(config));
        }
    }
}
