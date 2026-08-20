package cl.dynasty.nexusbeacon.platform.legacy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class LegacyEffectDefinitionRegistryTest {
    @Test void mapsAllNineteenConfiguredEffectsToTheSevenAuditedExecutorTypes() {
        LegacyEffectDefinitionRegistry registry = registry();
        Set<String> types = new HashSet<String>();
        for (LegacyEffectDefinition definition : registry.all()) types.add(definition.getType());

        assertEquals(19, registry.size());
        assertEquals(7, types.size());
        assertTrue(types.contains("POTION"));
        assertTrue(types.contains("CROP_BOOST"));
        assertTrue(types.contains("SPAWNER_BOOST"));
        assertTrue(types.contains("IGNITION"));
        assertTrue(types.contains("DAMAGE_FIELD"));
        assertTrue(types.contains("GRAVITY_PULSE"));
        assertTrue(types.contains("BLOCK_PROCESS_BOOST"));
    }

    @Test void rejectsModernFurnacesWithoutSubstitutingAnOrdinaryFurnace() {
        LegacyEffectDefinitionRegistry registry = registry();
        LegacyEffectDefinition modernFurnace = registry.get("modern_furnace_boost");

        assertNotNull(modernFurnace);
        assertFalse(modernFurnace.isSupported());
        assertTrue(modernFurnace.getTargetBlocks().isEmpty());
    }

    @Test void retainsConfiguredPotionDurationAmplifierAndTargets() {
        LegacyEffectDefinitionRegistry registry = registry();

        assertEquals(300, registry.get("night_vision").getPotionDurationTicks());
        assertEquals(1, registry.get("speed").getAmplifierPerLevel());
        assertEquals("MONSTERS", registry.get("poison").getTarget());
        assertTrue(registry.get("furnace_boost").isSupported());
        assertEquals(Material.FURNACE, registry.get("furnace_boost").getTargetBlocks().get(0));
    }

    @Test void invalidGameplayParametersFailClosedDuringBootstrap() throws Exception {
        FileConfiguration config = new YamlConfiguration();
        config.set("effects.bad.enabled", true);
        config.set("effects.bad.type", "CROP_BOOST");
        config.set("effects.bad.max-level", 1);
        config.set("effects.bad.levels.1.growth-chance", 101);

        assertThrows(IllegalArgumentException.class, () -> new LegacyEffectDefinitionRegistry(config,
                new LegacyMaterialResolver(), new LegacyPotionEffectResolver()));
    }

    private static LegacyEffectDefinitionRegistry registry() {
        File file = new File("../src/main/resources/effects.yml");
        if (!file.isFile()) file = new File("src/main/resources/effects.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        assertFalse(config.getKeys(false).isEmpty(), file.getAbsolutePath());
        return new LegacyEffectDefinitionRegistry(config,
                new LegacyMaterialResolver(), new LegacyPotionEffectResolver());
    }
}
