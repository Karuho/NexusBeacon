package cl.dynasty.nexusbeacon.platform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

class JavaFeatureVersionTest {
    @ParameterizedTest
    @CsvSource({"1.8,8", "8,8", "21,21", "21.0.7,21", "25,25"})
    void parsesFeatureVersion(String input, int expected) {
        assertEquals(expected, JavaFeatureVersion.parse(input));
    }

    @Test
    void rejectsInvalidFeatureVersion() {
        assertThrows(IllegalArgumentException.class, () -> JavaFeatureVersion.parse("current"));
    }
}
