package cl.dynasty.nexusbeacon.classic;

import static org.junit.jupiter.api.Assertions.*;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class ClassicDescriptorTest {
    @Test void descriptorPreservesProductAndFloorContract() throws Exception {
        InputStream stream = getClass().getClassLoader().getResourceAsStream("plugin.yml");
        assertNotNull(stream); byte[] bytes = new byte[4096]; int length = stream.read(bytes);
        String descriptor = new String(bytes, 0, length, StandardCharsets.UTF_8);
        assertTrue(descriptor.contains("name: NexusBeacon\n")); assertTrue(descriptor.contains("api-version: '1.13'"));
        assertTrue(descriptor.contains("main: cl.dynasty.nexusbeacon.classic.ClassicNexusBeaconPlugin"));
    }
}
