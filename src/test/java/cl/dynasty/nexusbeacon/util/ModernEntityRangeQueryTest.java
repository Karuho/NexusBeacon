package cl.dynasty.nexusbeacon.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import cl.dynasty.nexusbeacon.effects.PotionBeaconEffect;
import cl.dynasty.nexusbeacon.effects.executor.CropBoostExecutor;
import cl.dynasty.nexusbeacon.effects.executor.DamageFieldExecutor;
import cl.dynasty.nexusbeacon.effects.executor.GravityPulseExecutor;
import cl.dynasty.nexusbeacon.effects.executor.IgnitionExecutor;
import cl.dynasty.nexusbeacon.effects.executor.SpawnerBoostExecutor;

class ModernEntityRangeQueryTest {

    @Test
    void fullWorldVerticalRadiusCoversLargePositiveAndNegativeSeparation() {
        assertEquals(378.0D, ModernEntityRangeQuery.verticalRadius(-58.0D, -64, 320));
        assertTrue(ModernEntityRangeQuery.isInsideHorizontal(0.0D, 0.0D, 16));
        assertTrue(ModernEntityRangeQuery.isInsideHorizontal(8.0D, 8.0D, 16));
        assertFalse(ModernEntityRangeQuery.isInsideHorizontal(16.0D, 1.0D, 16));
    }

    @Test
    void everyEntityEffectUsesTheCorrectedSharedQuery() throws IOException {
        assertUsesSharedQuery(PotionBeaconEffect.class);
        assertUsesSharedQuery(IgnitionExecutor.class);
        assertUsesSharedQuery(DamageFieldExecutor.class);
        assertUsesSharedQuery(GravityPulseExecutor.class);
    }

    @Test
    void blockScannersRetainTheirConfiguredVerticalRadii() throws IOException {
        assertBytecodeContains(CropBoostExecutor.class, "performance.crop-boost.vertical-radius");
        assertBytecodeContains(SpawnerBoostExecutor.class, "performance.spawner-boost.vertical-radius");
    }

    private void assertUsesSharedQuery(Class<?> type) throws IOException {
        assertBytecodeContains(type, "cl/dynasty/nexusbeacon/util/ModernEntityRangeQuery");
    }

    private void assertBytecodeContains(Class<?> type, String expected) throws IOException {
        String resource = "/" + type.getName().replace('.', '/') + ".class";
        try (InputStream input = type.getResourceAsStream(resource)) {
            assertTrue(input != null, "missing bytecode: " + resource);
            String constantPool = new String(input.readAllBytes(), StandardCharsets.ISO_8859_1);
            assertTrue(constantPool.contains(expected), type.getSimpleName() + " missing " + expected);
        }
    }
}
