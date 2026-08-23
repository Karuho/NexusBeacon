package cl.dynasty.nexusbeacon.platform.classic;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import cl.dynasty.nexusbeacon.platform.MinecraftVersion;

class ClassicCapabilitiesTest {
    @Test void selectsCustomTagsAtFloor() { assertEquals(ClassicCapabilities.IdentityBackend.CUSTOM_ITEM_TAGS, caps("1.13.2").getIdentityBackend()); }
    @Test void selectsPdcFromOneFourteen() { assertTrue(caps("1.14.4-R0.1-SNAPSHOT").hasPersistentDataContainer()); }
    @Test void acceptsUpperEndpoint() { assertEquals(MinecraftVersion.parse("1.20.4"), caps("1.20.4").getVersion()); }
    @Test void rejectsVersionsOutsideContract() { assertThrows(IllegalArgumentException.class, () -> caps("1.21.1")); }
    @Test void baselineSchedulingAndTeleportAreExplicit() { assertTrue(caps("1.18.2").usesBukkitScheduler()); assertTrue(caps("1.18.2").usesSynchronousTeleport()); }
    private static ClassicCapabilities caps(String version) { return ClassicCapabilities.forVersion(MinecraftVersion.parse(version)); }
}
