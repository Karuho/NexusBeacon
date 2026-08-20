package cl.dynasty.nexusbeacon.platform.legacy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

class LegacyNbtBridgeFactoryTest {
    @Test
    void selects188BridgeWithoutLoadingIt() {
        assertEquals(
                "cl.dynasty.nexusbeacon.platform.legacy.bridge.v1_8_R3.LegacyNbtBridgeV1_8_R3",
                LegacyNbtBridgeFactory.resolveBridgeClassName("org.bukkit.craftbukkit.v1_8_R3"));
    }

    @Test
    void selects1122BridgeWithoutLoadingIt() {
        assertEquals(
                "cl.dynasty.nexusbeacon.platform.legacy.bridge.v1_12_R1.LegacyNbtBridgeV1_12_R1",
                LegacyNbtBridgeFactory.resolveBridgeClassName("org.bukkit.craftbukkit.v1_12_R1"));
    }

    @Test
    void rejectsUnknownRevisionExplicitly() {
        assertThrows(IllegalArgumentException.class,
                () -> LegacyNbtBridgeFactory.resolveBridgeClassName("org.bukkit.craftbukkit.v1_9_R2"));
    }
}
