package cl.dynasty.nexusbeacon.platform.legacy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class LegacyBeaconLocationTest {
    @Test void roundTripsWithoutResolvingABukkitWorld() {
        LegacyBeaconLocation location = LegacyBeaconLocation.parse("unloaded_world;-12;64;99");
        assertEquals("unloaded_world", location.getWorldName());
        assertEquals("unloaded_world;-12;64;99", location.toStorageKey());
    }

    @Test void rejectsMalformedOrOutOfLegacyBoundsLocations() {
        assertThrows(IllegalArgumentException.class, () -> LegacyBeaconLocation.parse("world;1;2"));
        assertThrows(IllegalArgumentException.class, () -> LegacyBeaconLocation.parse("world;1;-1;2"));
        assertThrows(IllegalArgumentException.class, () -> LegacyBeaconLocation.parse("world;30000001;64;2"));
        assertThrows(IllegalArgumentException.class, () -> LegacyBeaconLocation.parse("bad;world;1;2;3"));
        assertThrows(IllegalArgumentException.class, () -> LegacyBeaconLocation.parse("dotted.world;1;64;2"));
    }
}
