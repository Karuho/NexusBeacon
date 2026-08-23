package cl.dynasty.nexusbeacon.platform.legacy;

import java.io.File;
import java.util.Locale;

/** Selects only backends that are productively available on the Legacy path. */
public final class LegacyStorageFactory {
    private LegacyStorageFactory() { }

    public static LegacyBeaconStorage create(String configuredType, File storageFile) {
        String type = configuredType == null ? "" : configuredType.trim().toUpperCase(Locale.ROOT);
        if (!"YAML".equals(type)) {
            throw new IllegalArgumentException("Configured Legacy storage backend is unavailable: " + type);
        }
        return new LegacyYamlBeaconStorage(storageFile);
    }
}
