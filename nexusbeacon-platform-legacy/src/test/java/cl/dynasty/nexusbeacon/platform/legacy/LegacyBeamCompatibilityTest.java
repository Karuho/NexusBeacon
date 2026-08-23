package cl.dynasty.nexusbeacon.platform.legacy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.EnumMap;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.Test;

class LegacyBeamCompatibilityTest {
    @Test void classifiesAllFiveStylesIndependentlyOn18() {
        Map<LegacyBeamCompatibilityStatus, Integer> counts = classify(LegacyParticleRuntime.SPIGOT_1_8);

        assertEquals(5, LegacyBeamStylePlan.currentDefaults().size());
        assertEquals(4, count(counts, LegacyBeamCompatibilityStatus.VISUAL_DEGRADATION));
        assertEquals(1, count(counts, LegacyBeamCompatibilityStatus.UNSUPPORTED));
    }

    @Test void classifiesFourFixedSizeDustStylesAndExactEndRodOn112() {
        Map<LegacyBeamCompatibilityStatus, Integer> counts = classify(LegacyParticleRuntime.SPIGOT_1_12);

        assertEquals(4, count(counts, LegacyBeamCompatibilityStatus.VISUAL_DEGRADATION));
        assertEquals(1, count(counts, LegacyBeamCompatibilityStatus.FULL));
        assertEquals(0, count(counts, LegacyBeamCompatibilityStatus.UNSUPPORTED));
    }

    @Test void rendererSchedulesOneMainThreadBatchAndPreservesGeometryDensity() {
        ImmediateScheduler scheduler = new ImmediateScheduler();
        RecordingParticleTransport transport = new RecordingParticleTransport();
        LegacyParticleService particles = new LegacyParticleService(
                LegacyParticleRuntime.SPIGOT_1_8, scheduler, transport);
        LegacyBeamRenderer renderer = new LegacyBeamRenderer(particles, scheduler);
        World world = (World) Proxy.newProxyInstance(World.class.getClassLoader(),
                new Class<?>[] { World.class }, (proxy, method, args) -> {
                    if ("equals".equals(method.getName())) return proxy == args[0];
                    return null;
                });

        LegacyBeamRenderResult result = renderer.render(new Location(world, 0.5D, 65.0D, 0.5D),
                1, 0.5D, 1, LegacyBeamStylePlan.currentDefaults().get(0));

        assertTrue(result.isScheduled());
        assertEquals(3, result.getPoints());
        assertEquals(1, scheduler.syncCalls);
        assertEquals(3, transport.worldCalls);
        assertEquals(65.0D, transport.lastRequest.getLocation().getY() - 1.0D);
    }

    @Test void unsupportedStyleIsRejectedBeforeScheduling() {
        ImmediateScheduler scheduler = new ImmediateScheduler();
        LegacyParticleService particles = new LegacyParticleService(
                LegacyParticleRuntime.SPIGOT_1_8, scheduler, new RecordingParticleTransport());
        LegacyBeamRenderer renderer = new LegacyBeamRenderer(particles, scheduler);
        World world = (World) Proxy.newProxyInstance(World.class.getClassLoader(),
                new Class<?>[] { World.class }, (proxy, method, args) -> null);

        LegacyBeamRenderResult result = renderer.render(new Location(world, 0, 64, 0),
                10, 1.0D, 1, new LegacyBeamStylePlan("future", "SONIC_BOOM", null, 1.0F));

        assertFalse(result.isScheduled());
        assertEquals(LegacyBeamCompatibilityStatus.UNSUPPORTED, result.getCompatibility().getStatus());
        assertEquals(0, scheduler.syncCalls);
    }

    private static Map<LegacyBeamCompatibilityStatus, Integer> classify(LegacyParticleRuntime runtime) {
        LegacyBeamCompatibility compatibility = new LegacyBeamCompatibility(
                LegacyParticleResolutionTest.service(runtime));
        Map<LegacyBeamCompatibilityStatus, Integer> counts =
                new EnumMap<LegacyBeamCompatibilityStatus, Integer>(LegacyBeamCompatibilityStatus.class);
        for (LegacyBeamStylePlan style : LegacyBeamStylePlan.currentDefaults()) {
            LegacyBeamCompatibilityStatus status = compatibility.classify(style).getStatus();
            counts.put(status, Integer.valueOf(count(counts, status) + 1));
        }
        return counts;
    }

    private static int count(Map<LegacyBeamCompatibilityStatus, Integer> counts,
            LegacyBeamCompatibilityStatus status) {
        Integer value = counts.get(status);
        return value == null ? 0 : value.intValue();
    }
}
