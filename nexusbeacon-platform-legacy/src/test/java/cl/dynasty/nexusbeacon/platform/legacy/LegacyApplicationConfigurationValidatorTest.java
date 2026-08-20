package cl.dynasty.nexusbeacon.platform.legacy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class LegacyApplicationConfigurationValidatorTest {
    @Test void validatesApplicationMetadataThroughProductiveCompatibilityServices() {
        ConfigSet files = validFiles();

        LegacyApplicationConfiguration result = validator().validate(
                files.config, files.beacon, files.effects, files.gui);

        assertEquals("en_us", result.getLanguage());
        assertEquals("YAML", result.getStorageType());
        assertEquals("EXPONENTIAL", result.getRangeMode());
        assertEquals(2, result.getConfiguredEffects());
        assertEquals(0, result.getPotionEffects());
        assertEquals(2, result.getDeferredEffects());
        assertEquals(0, result.getUnsupportedEffects());
        assertEquals(2, result.getGuiItems());
        assertEquals(1, result.getGuiVisualFallbacks());
        assertEquals(1, result.getBeamStyles());
        assertTrue(result.isRecipeConfigured());
    }

    @Test void rejectsUnsupportedFunctionalParticleInsteadOfSilentlyDiscardingIt() {
        ConfigSet files = validFiles();
        files.beacon.set("beacon.particles.type", "SONIC_BOOM");

        assertThrows(IllegalArgumentException.class, () -> validator().validate(
                files.config, files.beacon, files.effects, files.gui));
    }

    @Test void parsesRecipeMetadataButNeverMakesRecipeCapabilityAvailable() {
        ConfigSet files = validFiles();
        LegacyApplicationConfiguration configuration = validator().validate(
                files.config, files.beacon, files.effects, files.gui);

        assertTrue(configuration.isRecipeConfigured());
        assertEquals(2, configuration.getDeferredEffects());
    }

    @Test void recordsUnsupportedPotionMetadataAsDeferredInsteadOfCrashingConfiguration() {
        ConfigSet files = validFiles();
        files.effects.set("effects.speed.type", "POTION");
        files.effects.set("effects.speed.potion", "DARKNESS");

        LegacyApplicationConfiguration configuration = validator().validate(
                files.config, files.beacon, files.effects, files.gui);

        assertEquals(1, configuration.getUnsupportedEffects());
        assertEquals(2, configuration.getDeferredEffects());
    }

    private static LegacyApplicationConfigurationValidator validator() {
        ImmediateScheduler scheduler = new ImmediateScheduler();
        LegacyParticleService particles = new LegacyParticleService(
                LegacyParticleRuntime.SPIGOT_1_8, scheduler, new RecordingParticleTransport());
        return new LegacyApplicationConfigurationValidator(new LegacyMaterialResolver(),
                new LegacyPotionEffectResolver(), particles, new LegacyBeamCompatibility(particles));
    }

    private static ConfigSet validFiles() {
        ConfigSet files = new ConfigSet();
        files.config.set("language", "en_us");
        files.config.set("storage.type", "YAML");
        files.beacon.set("item.material", "BEACON");
        files.beacon.set("beacon.range-calculator.mode", "EXPONENTIAL");
        files.beacon.set("beacon.particles.type", "VILLAGER_HAPPY");
        files.beacon.set("beam-styles.aqua.particle", "DUST");
        files.beacon.set("beam-styles.aqua.color", "AQUA");
        files.beacon.set("beam-styles.aqua.size", Double.valueOf(1.2D));
        files.beacon.set("recipe.enabled", Boolean.TRUE);
        files.beacon.set("recipe.shape", Arrays.asList("DDD", "DBD", "OOO"));
        files.beacon.set("recipe.ingredients.B", "BEACON");
        files.beacon.set("recipe.ingredients.D", "DIAMOND_BLOCK");
        files.beacon.set("recipe.ingredients.O", "OBSIDIAN");
        files.effects.set("effects.speed.enabled", Boolean.TRUE);
        files.effects.set("effects.speed.type", "DAMAGE_FIELD");
        files.effects.set("effects.speed.icon", "DIAMOND_BOOTS");
        files.effects.set("effects.crop_boost.enabled", Boolean.TRUE);
        files.effects.set("effects.crop_boost.type", "CROP_BOOST");
        files.effects.set("effects.crop_boost.icon", "WHEAT");
        files.gui.set("menus.main.items.B.material", "BEACON");
        files.gui.set("menus.main.items.X.material", "REINFORCED_DEEPSLATE");
        return files;
    }

    private static final class ConfigSet {
        private final YamlConfiguration config = new YamlConfiguration();
        private final YamlConfiguration beacon = new YamlConfiguration();
        private final YamlConfiguration effects = new YamlConfiguration();
        private final YamlConfiguration gui = new YamlConfiguration();
    }
}
