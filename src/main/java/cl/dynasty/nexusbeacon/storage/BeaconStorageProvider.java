package cl.dynasty.nexusbeacon.storage;

import java.util.List;

import cl.dynasty.nexusbeacon.model.BeaconData;

public interface BeaconStorageProvider {

    List<BeaconData> loadBeacons();

    void saveBeacon(BeaconData beacon);

    void removeBeacon(String id);

    void close();
}