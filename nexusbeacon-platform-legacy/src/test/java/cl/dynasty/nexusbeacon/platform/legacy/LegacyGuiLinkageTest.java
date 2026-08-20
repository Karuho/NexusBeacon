package cl.dynasty.nexusbeacon.platform.legacy;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class LegacyGuiLinkageTest {
    @Test void productiveGuiPathContainsNoModernOnlyLinkage() throws Exception {
        for (Class<?> type : new Class<?>[] { LegacyGuiController.class, LegacyGuiInteractionListener.class,
                LegacyGuiHolder.class, LegacyGuiSession.class }) {
            String constants = constants(type);
            assertFalse(constants.contains("net/kyori"), type.getName());
            assertFalse(constants.contains("PlayerProfile"), type.getName());
            assertFalse(constants.contains("PersistentDataContainer"), type.getName());
            assertFalse(constants.contains("CustomModelData"), type.getName());
            assertFalse(constants.contains("BlockData"), type.getName());
            assertFalse(constants.contains("ModernAdapter"), type.getName());
        }
    }

    private static String constants(Class<?> type) throws Exception {
        String resource = "/" + type.getName().replace('.', '/') + ".class";
        InputStream input = type.getResourceAsStream(resource);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        for (int count; (count = input.read(buffer)) >= 0; ) bytes.write(buffer, 0, count);
        input.close();
        return new String(bytes.toByteArray(), StandardCharsets.ISO_8859_1);
    }
}
