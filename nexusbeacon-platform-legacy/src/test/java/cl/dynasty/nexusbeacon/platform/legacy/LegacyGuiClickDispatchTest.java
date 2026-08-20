package cl.dynasty.nexusbeacon.platform.legacy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LegacyGuiClickDispatchTest {
    @Test void onlyExplicitSemanticClicksMayDispatchGuiActions() {
        assertTrue(LegacyGuiInteractionListener.isSemanticClick("LEFT"));
        assertTrue(LegacyGuiInteractionListener.isSemanticClick("RIGHT"));
        assertFalse(LegacyGuiInteractionListener.isSemanticClick("NUMBER_KEY"));
        assertFalse(LegacyGuiInteractionListener.isSemanticClick("DOUBLE_CLICK"));
        assertFalse(LegacyGuiInteractionListener.isSemanticClick("DROP"));
        assertFalse(LegacyGuiInteractionListener.isSemanticClick("CONTROL_DROP"));
        assertFalse(LegacyGuiInteractionListener.isSemanticClick("SHIFT_LEFT"));
    }
}
