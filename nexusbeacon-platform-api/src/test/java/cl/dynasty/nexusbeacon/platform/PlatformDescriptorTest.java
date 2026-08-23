package cl.dynasty.nexusbeacon.platform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class PlatformDescriptorTest {
    @Test
    void valueIsDeterministic() {
        PlatformDescriptor first = new PlatformDescriptor(MinecraftVersion.parse("1.21.1"), 21, "Paper");
        PlatformDescriptor second = new PlatformDescriptor(MinecraftVersion.parse("1.21.1"), 21, "Paper");
        PlatformDescriptor different = new PlatformDescriptor(MinecraftVersion.parse("26.2"), 25, "Paper");

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first, different);
    }
}
