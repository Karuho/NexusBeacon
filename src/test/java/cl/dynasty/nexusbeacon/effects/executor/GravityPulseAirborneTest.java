package cl.dynasty.nexusbeacon.effects.executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

class GravityPulseAirborneTest {

    @Test
    void groundedAirborneAndLargeVerticalSeparationsReceiveSameHorizontalPull() {
        Vector target = new Vector(0.5D, -57.0D, 0.5D);
        Vector grounded = velocity(new Vector(8.0D, -57.0D, 0.5D), new Vector(), target);
        Vector highAirborne = velocity(new Vector(8.0D, 300.0D, 0.5D), new Vector(), target);
        Vector lowAirborne = velocity(new Vector(8.0D, -63.0D, 0.5D), new Vector(), target);

        assertEquals(grounded.getX(), highAirborne.getX(), 0.000001D);
        assertEquals(grounded.getX(), lowAirborne.getX(), 0.000001D);
        assertTrue(grounded.getX() < 0.0D);
        assertEquals(0.05D, highAirborne.getY(), 0.000001D);
    }

    @Test
    void fallingOverOpenAirRemainsAttractedWhileVanillaFallVelocityContinues() {
        Vector falling = velocity(
                new Vector(8.0D, 200.0D, 0.5D), new Vector(0.0D, -0.4D, 0.0D),
                new Vector(0.5D, -57.0D, 0.5D));

        assertTrue(falling.getX() < 0.0D);
        assertEquals(-0.35D, falling.getY(), 0.000001D);
    }

    @Test
    void pullStrengthVerticalBoostAndMaximumVelocityKeepTheirNumericMeaning() {
        Vector uncapped = GravityPulseExecutor.attractionVelocity(
                new Vector(), new Vector(10.0D, 100.0D, 0.0D), new Vector(),
                0.2D, 0.07D, 2.0D);
        assertEquals(-0.2D, uncapped.getX(), 0.000001D);
        assertEquals(0.07D, uncapped.getY(), 0.000001D);

        Vector capped = GravityPulseExecutor.attractionVelocity(
                new Vector(2.0D, 0.0D, 0.0D), new Vector(10.0D, 100.0D, 0.0D), new Vector(),
                0.2D, 0.07D, 0.5D);
        assertEquals(0.5D, capped.length(), 0.000001D);
    }

    @Test
    void executorHasNoGroundOrSupportBlockEligibilityDependency() throws Exception {
        String resource = "/" + GravityPulseExecutor.class.getName().replace('.', '/') + ".class";
        try (InputStream input = GravityPulseExecutor.class.getResourceAsStream(resource)) {
            String bytecode = new String(input.readAllBytes(), StandardCharsets.ISO_8859_1);
            assertFalse(bytecode.contains("isOnGround"));
            assertFalse(bytecode.contains("getBlock"));
            assertFalse(bytecode.contains("isPassable"));
            assertTrue(bytecode.contains("ModernEntityRangeQuery"));
        }
    }

    private Vector velocity(Vector position, Vector currentVelocity, Vector target) {
        return GravityPulseExecutor.attractionVelocity(
                currentVelocity, position, target, 0.08D, 0.05D, 1.2D);
    }
}
