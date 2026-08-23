package cl.dynasty.nexusbeacon.platform.legacy;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class LegacyBukkitCommandEnvironmentLinkageTest {
    @Test void doesNotLinkVersionSpecificTargetBlockOverloads() throws Exception {
        String resource = "/" + LegacyBukkitCommandEnvironment.class.getName().replace('.', '/') + ".class";
        InputStream input = LegacyBukkitCommandEnvironment.class.getResourceAsStream(resource);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        for (int count; (count = input.read(buffer)) >= 0; ) bytes.write(buffer, 0, count);
        input.close();
        String constantPool = new String(bytes.toByteArray(), StandardCharsets.ISO_8859_1);
        assertFalse(constantPool.contains("getTargetBlock"));
        assertFalse(constantPool.contains("getLastTwoTargetBlocks"));
    }
}
