package cl.dynasty.nexusbeacon.platform.legacy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.Effect;
import org.junit.jupiter.api.Test;

class LegacyParticleResolutionTest {
    @Test void resolvesEveryCurrentRangeParticleThroughStableEffects() {
        LegacyParticleService service = service(LegacyParticleRuntime.SPIGOT_1_8);

        assertEffect(service, "VILLAGER_HAPPY", Effect.HAPPY_VILLAGER);
        assertEffect(service, "FLAME", Effect.FLAME);
        assertEffect(service, "CRIT", Effect.CRIT);
        assertEffect(service, "CLOUD", Effect.CLOUD);
        assertEffect(service, "PORTAL", Effect.PORTAL);
    }

    @Test void preservesExplicitHistoricalAliasesWithoutFuzzyMatching() {
        LegacyParticleResolution happy = service(LegacyParticleRuntime.SPIGOT_1_8).resolve("happy-villager");
        LegacyParticleResolution redstone = service(LegacyParticleRuntime.SPIGOT_1_8).resolve("REDSTONE");

        assertEquals(LegacyParticleCompatibility.LEGACY_ALIAS, happy.getCompatibility());
        assertEquals("VILLAGER_HAPPY", happy.getSemanticName());
        assertEquals(LegacyParticleCompatibility.LEGACY_ALIAS, redstone.getCompatibility());
        assertEquals("DUST", redstone.getSemanticName());
    }

    @Test void distinguishesKnownModernOnlyParticleFromInvalidInput() {
        LegacyParticleService service = service(LegacyParticleRuntime.SPIGOT_1_8);

        assertEquals(LegacyParticleCompatibility.UNSUPPORTED,
                service.resolve("SONIC_BOOM").getCompatibility());
        assertEquals(LegacyParticleCompatibility.INVALID,
                service.resolve("not-a-real-particle").getCompatibility());
        assertEquals(LegacyParticleCompatibility.INVALID, service.resolve(null).getCompatibility());
    }

    @Test void endRodIsExactOn112AndExplicitlyApproximateOn18() {
        LegacyParticleResolution old = service(LegacyParticleRuntime.SPIGOT_1_8).resolve("END_ROD");
        LegacyParticleResolution newer = service(LegacyParticleRuntime.SPIGOT_1_12).resolve("END_ROD");

        assertEquals(LegacyParticleCompatibility.VISUAL_APPROXIMATION, old.getCompatibility());
        assertEquals("FIREWORKS_SPARK", old.getPhysicalName());
        assertEquals(LegacyParticleCompatibility.EXACT, newer.getCompatibility());
        assertEquals("END_ROD", newer.getPhysicalName());
    }

    @Test void dustReportsExactRgbAndFixedLegacySize() {
        LegacyParticleResolution dust = service(LegacyParticleRuntime.SPIGOT_1_8).resolve("DUST");

        assertTrue(dust.isColorSupported());
        assertFalse(dust.isSizeSupported());
        assertTrue(dust.isVisuallyDegraded());
    }

    @Test void runtimeSelectionIsStrict() {
        assertEquals(LegacyParticleRuntime.SPIGOT_1_8,
                LegacyParticleRuntime.fromCraftPackage("org.bukkit.craftbukkit.v1_8_R3"));
        assertEquals(LegacyParticleRuntime.SPIGOT_1_12,
                LegacyParticleRuntime.fromCraftPackage("org.bukkit.craftbukkit.v1_12_R1"));
    }

    private static void assertEffect(LegacyParticleService service, String name, Effect effect) {
        LegacyParticleResolution resolution = service.resolve(name);
        assertTrue(resolution.isSupported());
        assertEquals(LegacyParticleCompatibility.LEGACY_EFFECT_EQUIVALENT,
                resolution.getCompatibility());
        assertEquals(effect, resolution.getEffect());
    }

    static LegacyParticleService service(LegacyParticleRuntime runtime) {
        return new LegacyParticleService(runtime, new ImmediateScheduler(), new RecordingParticleTransport());
    }
}
