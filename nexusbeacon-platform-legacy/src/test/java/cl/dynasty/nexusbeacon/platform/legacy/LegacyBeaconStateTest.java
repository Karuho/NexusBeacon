package cl.dynasty.nexusbeacon.platform.legacy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class LegacyBeaconStateTest {
    @Test void ownsImmutableNormalizedCollections() {
        Map<String, Integer> effects = new HashMap<String, Integer>();
        effects.put("SPEED", Integer.valueOf(2));
        Set<String> active = new HashSet<String>();
        active.add("Speed");
        LegacyBeaconState state = state(effects, active);
        effects.clear();
        active.clear();
        assertEquals(Integer.valueOf(2), state.getEffectLevels().get("speed"));
        assertEquals(Collections.singleton("speed"), state.getActiveEffects());
        assertThrows(UnsupportedOperationException.class,
                () -> state.getEffectLevels().put("haste", Integer.valueOf(1)));
    }

    @Test void rejectsActiveEffectsWithoutAnAcquiredLevel() {
        assertThrows(IllegalArgumentException.class, () -> state(Collections.<String, Integer>emptyMap(),
                Collections.singleton("speed")));
    }

    private static LegacyBeaconState state(Map<String, Integer> effects, Set<String> active) {
        return new LegacyBeaconState(new LegacyBeaconLocation("world", 1, 64, 2), UUID.randomUUID(),
                UUID.randomUUID(), 48, 1, effects, active, Collections.<UUID>emptySet(), true,
                "aqua", true, "VILLAGER_HAPPY");
    }
}
