package cl.dynasty.nexusbeacon.platform.legacy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LegacyStorageRestartTest {
    @TempDir Path temporaryDirectory;

    @Test void secondApplicationBootLoadsTheFirstBootSnapshotSemantically() {
        Path file = temporaryDirectory.resolve("storage.yml");
        LegacyApplicationState bootA = new LegacyApplicationState(new LegacyYamlBeaconStorage(file.toFile()));
        bootA.initialize();
        LegacyBeaconState expected = LegacyYamlBeaconStorageTest.representative("world", 8);
        assertTrue(bootA.insert(expected));
        bootA.close();

        LegacyApplicationState bootB = new LegacyApplicationState(new LegacyYamlBeaconStorage(file.toFile()));
        assertEquals(LegacyStorageLoadStatus.READY, bootB.initialize().getStatus());
        assertEquals(Collections.singletonList(expected), bootB.snapshot());
    }
}
