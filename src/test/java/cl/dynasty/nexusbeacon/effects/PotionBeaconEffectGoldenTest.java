package cl.dynasty.nexusbeacon.effects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class PotionBeaconEffectGoldenTest {
    @Test
    void preservesDurationAmplifierAmbientAndParticles() {
        PotionBeaconEffect effect = new PotionBeaconEffect(
                "speed", "Speed", List.of("description"), Material.DIAMOND_BOOTS,
                null, "PLAYERS", 1, 100, 3, 2);

        assertEquals(100, effect.getDurationTicks());
        assertEquals(0, effect.calculateAmplifier(1));
        assertEquals(2, effect.calculateAmplifier(3));
        assertTrue(effect.isAmbient());
        assertTrue(effect.hasParticles());
    }

    @Test
    void amplifierNeverFallsBelowZero() {
        PotionBeaconEffect effect = new PotionBeaconEffect(
                "speed", "Speed", List.of(), Material.DIAMOND_BOOTS,
                null, "PLAYERS", 0, 80, 1, 1);
        assertEquals(0, effect.calculateAmplifier(0));
    }
}
