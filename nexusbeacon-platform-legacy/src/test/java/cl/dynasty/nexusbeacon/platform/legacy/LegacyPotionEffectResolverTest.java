package cl.dynasty.nexusbeacon.platform.legacy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.bukkit.potion.PotionEffectType;
import org.junit.jupiter.api.Test;

import cl.dynasty.nexusbeacon.platform.api.PotionEffectResolution;

class LegacyPotionEffectResolverTest {
    @Test void sendsConfiguredLegacyEffectToRuntimeRegistry() {
        assertLookup("SPEED", "SPEED");
    }

    @Test void mapsModernStrengthNameToLegacyApiName() {
        assertLookup("strength", "INCREASE_DAMAGE");
    }

    @Test void mapsBoundedCompatibilityAliases() {
        assertLookup("haste", "FAST_DIGGING");
        assertLookup("resistance", "DAMAGE_RESISTANCE");
        assertLookup("slowness", "SLOW");
        assertLookup("jump-boost", "JUMP");
        assertLookup("nausea", "CONFUSION");
        assertLookup("instant health", "HEAL");
        assertLookup("instant_damage", "HARM");
    }

    @Test void normalizesNamespacedIdentifier() {
        assertLookup(" minecraft:speed ", "SPEED");
    }

    @Test void reportsUnknownValidNameAsUnsupported() {
        PotionEffectResolution result = new LegacyPotionEffectResolver().resolvePotionEffect("DARKNESS");
        assertEquals(PotionEffectResolution.Status.UNSUPPORTED, result.getStatus());
    }

    @Test void rejectsMalformedAndBlankIdentifiers() {
        LegacyPotionEffectResolver resolver = new LegacyPotionEffectResolver();
        assertEquals(PotionEffectResolution.Status.INVALID_IDENTIFIER, resolver.resolvePotionEffect("bad/id").getStatus());
        assertEquals(PotionEffectResolution.Status.INVALID_IDENTIFIER, resolver.resolvePotionEffect(" ").getStatus());
    }

    private void assertLookup(String input, String expected) {
        final AtomicReference<String> actual = new AtomicReference<String>();
        LegacyPotionEffectResolver resolver = new LegacyPotionEffectResolver(new Function<String, PotionEffectType>() {
            @Override public PotionEffectType apply(String name) {
                actual.set(name);
                return null;
            }
        });
        resolver.resolvePotionEffect(input);
        assertEquals(expected, actual.get());
    }
}
