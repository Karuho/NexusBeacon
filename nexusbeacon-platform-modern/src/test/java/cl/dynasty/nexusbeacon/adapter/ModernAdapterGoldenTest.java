package cl.dynasty.nexusbeacon.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicReference;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import cl.dynasty.nexusbeacon.platform.api.MaterialContext;
import cl.dynasty.nexusbeacon.platform.api.MaterialResolution;
import cl.dynasty.nexusbeacon.platform.api.MaterialResolutionStatus;
import cl.dynasty.nexusbeacon.platform.api.PotionEffectResolution;

class ModernAdapterGoldenTest {
    private final ModernAdapter adapter = new ModernAdapter();

    @ParameterizedTest
    @CsvSource({
            "BEACON,BEACON",
            "diamond,DIAMOND",
            "experience bottle,EXPERIENCE_BOTTLE",
            "minecraft:sunflower,SUNFLOWER",
            "black-stained-glass-pane,BLACK_STAINED_GLASS_PANE",
            "NETHERITE_BLOCK,NETHERITE_BLOCK",
            "REINFORCED_DEEPSLATE,REINFORCED_DEEPSLATE"
    })
    void resolvesConfiguredModernMaterials(String identifier, Material expected) {
        MaterialResolution resolution = adapter.resolveMaterial(identifier, MaterialContext.REQUIRED_ITEM);
        assertTrue(resolution.isResolved());
        assertFalse(resolution.isFallbackUsed());
        assertEquals(expected, resolution.getMaterial().orElseThrow());
    }

    @Test
    void invalidGuiIconUsesOnlyExplicitVisualFallback() {
        MaterialResolution resolution = adapter.resolveMaterial("not/a/material", MaterialContext.GUI_ICON);
        assertEquals(MaterialResolutionStatus.INVALID_IDENTIFIER, resolution.getStatus());
        assertTrue(resolution.isFallbackUsed());
        assertEquals(Material.STONE, resolution.getMaterial().orElseThrow());
    }

    @ParameterizedTest
    @ValueSource(strings = {"RECIPE_INGREDIENT", "PAYMENT", "REQUIRED_ITEM", "BLOCK_MATCH", "EFFECT_ICON"})
    void requiredContextsNeverFallbackToStone(MaterialContext context) {
        MaterialResolution resolution = adapter.resolveMaterial("FUTURE_VALID_NAME", context);
        assertEquals(MaterialResolutionStatus.UNSUPPORTED, resolution.getStatus());
        assertFalse(resolution.isFallbackUsed());
        assertTrue(resolution.getMaterial().isEmpty());
    }

    @Test
    void blankIdentifierIsInvalidRatherThanUnsupported() {
        MaterialResolution resolution = adapter.resolveMaterial("  ", MaterialContext.PAYMENT);
        assertEquals(MaterialResolutionStatus.INVALID_IDENTIFIER, resolution.getStatus());
        assertTrue(resolution.getMaterial().isEmpty());
    }

    @Test
    void compatibilityMaterialMethodNoLongerSilentlyReturnsStone() {
        assertNull(adapter.material("FUTURE_VALID_NAME"));
    }

    @ParameterizedTest
    @CsvSource({
            "SPEED,SPEED",
            "regeneration,REGENERATION",
            "minecraft:night_vision,NIGHT_VISION",
            "fast-digging,FAST_DIGGING",
            "damage resistance,DAMAGE_RESISTANCE",
            "SLOW,SLOW"
    })
    void normalizesConfiguredPotionEffectsBeforePlatformLookup(String identifier, String expectedLookup) {
        AtomicReference<String> lookup = new AtomicReference<>();
        ModernAdapter recordingAdapter = new ModernAdapter(value -> {
            lookup.set(value);
            return null;
        });
        PotionEffectResolution resolution = recordingAdapter.resolvePotionEffect(identifier);
        assertEquals(expectedLookup, lookup.get());
        assertEquals(PotionEffectResolution.Status.UNSUPPORTED, resolution.getStatus());
    }

    @Test
    void unknownAndMalformedPotionEffectsAreDistinct() {
        ModernAdapter unavailableAdapter = new ModernAdapter(value -> null);
        assertEquals(PotionEffectResolution.Status.UNSUPPORTED,
                unavailableAdapter.resolvePotionEffect("FUTURE_EFFECT").getStatus());
        assertEquals(PotionEffectResolution.Status.INVALID_IDENTIFIER,
                unavailableAdapter.resolvePotionEffect("not/an/effect").getStatus());
    }
}
