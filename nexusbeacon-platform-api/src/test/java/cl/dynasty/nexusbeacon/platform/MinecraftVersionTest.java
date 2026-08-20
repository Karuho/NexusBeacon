package cl.dynasty.nexusbeacon.platform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

class MinecraftVersionTest {
    @ParameterizedTest
    @CsvSource({"1.8.8,1,8,8", "1.12.2,1,12,2", "1.13.2,1,13,2", "1.21.1,1,21,1", "26.2,26,2,0", "1.21.1-R0.1-SNAPSHOT,1,21,1"})
    void parsesSupportedVersionShapes(String input, int major, int minor, int patch) {
        MinecraftVersion version = MinecraftVersion.parse(input);
        assertEquals(major, version.getMajor());
        assertEquals(minor, version.getMinor());
        assertEquals(patch, version.getPatch());
    }

    @Test
    void rejectsNonVersionText() {
        assertThrows(MalformedMinecraftVersionException.class, () -> MinecraftVersion.parse("Paper"));
    }

    @Test
    void ordersHistoricalModernAndCalendarVersionsNumerically() {
        assertTrue(MinecraftVersion.parse("1.12.2").compareTo(MinecraftVersion.parse("1.13.0")) < 0);
        assertTrue(MinecraftVersion.parse("1.13.2").compareTo(MinecraftVersion.parse("1.21.0")) < 0);
        assertTrue(MinecraftVersion.parse("1.21.9").compareTo(MinecraftVersion.parse("26.2")) < 0);
    }

    @Test
    void patchParticipatesInOrdering() {
        assertTrue(MinecraftVersion.parse("1.21.1").compareTo(MinecraftVersion.parse("1.21.2")) < 0);
        assertEquals(0, MinecraftVersion.parse("26.2").compareTo(MinecraftVersion.parse("26.2.0")));
    }
}
