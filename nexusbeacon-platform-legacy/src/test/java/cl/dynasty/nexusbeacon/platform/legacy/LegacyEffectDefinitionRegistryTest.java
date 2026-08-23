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
import org.bukkit.potion.PotionEffectType;
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

    @Test void supportedViewOmitsImpossibleDefinitionsAndRetainsFurnaceBoost() {
        LegacyEffectDefinitionRegistry registry = registry();
        Set<String> visible = new HashSet<String>();
        for (LegacyEffectDefinition definition : registry.supported()) visible.add(definition.getId());

        assertFalse(visible.contains("luck"));
        assertFalse(visible.contains("modern_furnace_boost"));
        assertTrue(visible.contains("furnace_boost"));
        assertEquals(17, visible.size());
    }

    @Test void luckIsRetainedWhenTheRuntimePotionExistsButSmokerDefinitionStillIsNot() {
        FileConfiguration config = effectsConfig();
        LegacyPotionEffectResolver potions = testPotions(true);
        LegacyEffectDefinitionRegistry registry = new LegacyEffectDefinitionRegistry(config,
                new LegacyMaterialResolver(), potions);
        Set<String> visible = new HashSet<String>();
        for (LegacyEffectDefinition definition : registry.supported()) visible.add(definition.getId());

        assertTrue(visible.contains("luck"));
        assertFalse(visible.contains("modern_furnace_boost"));
        assertTrue(visible.contains("furnace_boost"));
        assertEquals(18, visible.size());
    }

    @Test void resolvesConfiguredIconsBeforeUsingExplicitVisualFallback() {
        LegacyEffectDefinitionRegistry registry = registry();

        assertEquals(LegacyMaterialMappingKind.EXACT, registry.get("speed").getIconMappingKind());
        assertEquals(LegacyMaterialMappingKind.COMPATIBLE_ALIAS,
                registry.get("night_vision").getIconMappingKind());
        assertEquals(LegacyMaterialMappingKind.COMPATIBLE_ALIAS,
                registry.get("water_breathing").getIconMappingKind());
        assertEquals("TURTLE_HELMET", registry.get("water_breathing").getPresentationIcon());
        assertEquals(LegacyMaterialMappingKind.COMPATIBLE_ALIAS,
                registry.get("damage_field").getIconMappingKind());
        assertEquals("GOLDEN_CARROT", registry.get("luck").getIcon());
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
        return new LegacyEffectDefinitionRegistry(effectsConfig(),
                new LegacyMaterialResolver(), testPotions(false));
    }

    private static LegacyPotionEffectResolver testPotions(final boolean luckAvailable) {
        return new LegacyPotionEffectResolver(name ->
                "LUCK".equals(name) && !luckAvailable ? null : PotionEffectType.SPEED);
    }

    private static FileConfiguration effectsConfig() {
        File file = new File("../src/main/resources/effects.yml");
        if (!file.isFile()) file = new File("src/main/resources/effects.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        assertFalse(config.getKeys(false).isEmpty(), file.getAbsolutePath());
        return config;
    }
}
