package cl.dynasty.nexusbeacon.platform.legacy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LegacyStorageFactoryTest {
    @TempDir Path temporaryDirectory;

    @Test void selectsTheProductiveYamlBackend() {
        LegacyBeaconStorage storage = LegacyStorageFactory.create("yaml",
                temporaryDirectory.resolve("storage.yml").toFile());
        assertEquals("YAML", storage.getBackendName());
    }

    @Test void rejectsConfiguredDatabaseBackendsThatAreNotAvailableOnLegacy() {
        assertThrows(IllegalArgumentException.class, () -> LegacyStorageFactory.create("SQLITE",
                temporaryDirectory.resolve("storage.db").toFile()));
        assertThrows(IllegalArgumentException.class, () -> LegacyStorageFactory.create("MYSQL",
                temporaryDirectory.resolve("storage.yml").toFile()));
    }
}
