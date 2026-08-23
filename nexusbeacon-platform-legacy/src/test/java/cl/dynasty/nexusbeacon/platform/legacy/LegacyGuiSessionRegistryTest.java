package cl.dynasty.nexusbeacon.platform.legacy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class LegacyGuiSessionRegistryTest {
    @Test void replacementMakesOldCloseEventHarmlessAndQuitClearsCurrentSession() {
        LegacyGuiSessionRegistry sessions = new LegacyGuiSessionRegistry();
        UUID player = UUID.randomUUID();
        LegacyBeaconState beacon = beacon(UUID.randomUUID(), player);

        LegacyGuiSession first = sessions.replace(player, beacon, LegacyGuiMenu.MAIN);
        LegacyGuiSession second = sessions.replace(player, beacon, LegacyGuiMenu.EFFECTS);

        assertFalse(sessions.removeIfCurrent(first));
        assertTrue(sessions.isCurrent(second));
        sessions.remove(player);
        assertEquals(0, sessions.size());
    }

    @Test void sessionContainsIdentifiersButNoPlayerReference() {
        UUID player = UUID.randomUUID();
        LegacyBeaconState beacon = beacon(UUID.randomUUID(), player);
        LegacyGuiSession session = new LegacyGuiSessionRegistry().replace(player, beacon, LegacyGuiMenu.MAIN);

        assertEquals(player, session.getPlayerId());
        assertEquals(beacon.getUniqueId(), session.getBeaconId());
        assertEquals(beacon.getLocation(), session.getLocation());
    }

    private static LegacyBeaconState beacon(UUID id, UUID owner) {
        return new LegacyBeaconState(new LegacyBeaconLocation("world", 1, 64, 2), id, owner, 48, 1,
                Collections.<String, Integer>emptyMap(), Collections.<String>emptySet(),
                Collections.<UUID>emptySet(), true, "aqua", true, "VILLAGER_HAPPY");
    }
}
