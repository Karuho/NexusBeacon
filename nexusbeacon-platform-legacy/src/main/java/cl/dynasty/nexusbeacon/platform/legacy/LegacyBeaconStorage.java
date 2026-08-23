package cl.dynasty.nexusbeacon.platform.legacy;

import java.util.Collection;

public interface LegacyBeaconStorage {
    LegacyStorageLoadResult load();
    void store(Collection<LegacyBeaconState> beacons);
    void close();
    String getBackendName();
}
