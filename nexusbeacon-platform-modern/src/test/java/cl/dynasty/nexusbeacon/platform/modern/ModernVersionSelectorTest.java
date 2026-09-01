package cl.dynasty.nexusbeacon.platform.modern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import cl.dynasty.nexusbeacon.platform.MalformedMinecraftVersionException;
import cl.dynasty.nexusbeacon.platform.MinecraftVersion;

class ModernVersionSelectorTest {
    @ParameterizedTest
    @CsvSource({
            "1.13.2,8,MODERN_1_13,8",
            "1.13.2,21,MODERN_1_13,8",
            "1.20.5,21,MODERN_1_20_5,21",
            "1.20.6,21,MODERN_1_20_5,21",
            "1.21.1,21,MODERN_1_21,21",
            "1.21.1,25,MODERN_1_21,21",
            "26.2,25,MODERN_26_2,25"
    })
    void selectsValidatedModernFamily(String minecraft, int javaFeature,
            ModernVersionFamily family, int minimumJava) {
        ModernVersionSelection selection = select(minecraft, javaFeature);
        assertTrue(selection.isSupported());
        assertEquals(ModernSelectionStatus.SUPPORTED, selection.getStatus());
        assertEquals(family, selection.getProfile().orElseThrow().getFamily());
        assertEquals(minimumJava, selection.getProfile().orElseThrow().getMinimumJavaFeature());
    }

    @Test
    void newerJavaDoesNotChangeMinecraftFamily() {
        ModernVersionFamily java21 = select("1.21.1", 21).getProfile().orElseThrow().getFamily();
        ModernVersionFamily java25 = select("1.21.1", 25).getProfile().orElseThrow().getFamily();
        assertEquals(ModernVersionFamily.MODERN_1_21, java21);
        assertEquals(java21, java25);
    }

    @Test
    void reportsInsufficientJavaSeparatelyFromMinecraftSupport() {
        ModernVersionSelection selection = select("26.2", 8);
        assertFalse(selection.isSupported());
        assertEquals(ModernSelectionStatus.UNSUPPORTED_JAVA, selection.getStatus());
        assertEquals(ModernVersionFamily.MODERN_26_2, selection.getProfile().orElseThrow().getFamily());
        assertEquals(25, selection.getProfile().orElseThrow().getMinimumJavaFeature());
    }

    @ParameterizedTest
    @CsvSource({"1.12.2,8", "1.14.4,8", "1.20.4,21", "26.1,25", "26.3,25", "27.0,25"})
    void unknownOrUnvalidatedMinecraftVersionsFailSafely(String minecraft, int javaFeature) {
        ModernVersionSelection selection = select(minecraft, javaFeature);
        assertFalse(selection.isSupported());
        assertEquals(ModernSelectionStatus.UNSUPPORTED_MINECRAFT, selection.getStatus());
        assertTrue(selection.getProfile().isEmpty());
    }

    @Test
    void malformedVersionRemainsDistinctFromUnsupportedVersion() {
        assertThrows(MalformedMinecraftVersionException.class, () -> MinecraftVersion.parse("Paper-current"));
    }

    @Test
    void rejectsInvalidJavaFeatureInput() {
        assertThrows(IllegalArgumentException.class,
                () -> ModernVersionSelector.select(MinecraftVersion.parse("1.21.1"), 0));
    }

    private ModernVersionSelection select(String minecraft, int javaFeature) {
        return ModernVersionSelector.select(MinecraftVersion.parse(minecraft), javaFeature);
    }
}
