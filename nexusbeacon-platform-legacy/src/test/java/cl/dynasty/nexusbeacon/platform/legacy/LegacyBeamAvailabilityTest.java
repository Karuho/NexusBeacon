package cl.dynasty.nexusbeacon.platform.legacy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class LegacyBeamAvailabilityTest {
    @Test void rejectedEndRodStyleIsOmittedOn18() {
        List<LegacyBeamStylePlan> styles = compatibility(LegacyParticleRuntime.SPIGOT_1_8)
                .selectable(LegacyBeamStylePlan.currentDefaults());

        assertEquals(4, styles.size());
        assertFalse(contains(styles, "end_rod"));
        assertTrue(contains(styles, LegacyBeamStylePlan.DEFAULT_STYLE_ID));
    }

    @Test void exactEndRodStyleRemainsSelectableOn112() {
        List<LegacyBeamStylePlan> styles = compatibility(LegacyParticleRuntime.SPIGOT_1_12)
                .selectable(LegacyBeamStylePlan.currentDefaults());

        assertEquals(5, styles.size());
        assertTrue(contains(styles, "end_rod"));
    }

    @Test void defaultStyleMatchesModernGlobalAquaSemantics() {
        assertEquals("aqua", LegacyBeamStylePlan.defaultStyle().getId());
        assertEquals("DUST", LegacyBeamStylePlan.defaultStyle().getParticleName());
    }

    private static LegacyBeamCompatibility compatibility(LegacyParticleRuntime runtime) {
        ImmediateScheduler scheduler = new ImmediateScheduler();
        return new LegacyBeamCompatibility(new LegacyParticleService(runtime, scheduler,
                new RecordingParticleTransport()));
    }

    private static boolean contains(List<LegacyBeamStylePlan> styles, String id) {
        for (LegacyBeamStylePlan style : styles) if (id.equals(style.getId())) return true;
        return false;
    }
}
