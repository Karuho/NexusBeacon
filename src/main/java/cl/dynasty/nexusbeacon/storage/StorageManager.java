package cl.dynasty.nexusbeacon.storage;

import java.util.List;
import java.util.Map;

import cl.dynasty.nexusbeacon.NexusBeaconPlugin;
import cl.dynasty.nexusbeacon.model.BeaconData;

public class StorageManager {

    private final NexusBeaconPlugin plugin;
    private final BeaconStorageProvider provider;

    public StorageManager(NexusBeaconPlugin plugin) {
        this.plugin = plugin;
        this.provider = createProvider();
    }

    private BeaconStorageProvider createProvider() {
        String type = plugin.getConfigManager()
                .getConfig()
                .getString("storage.type", "YAML");

        return createProvider(type);
    }

    private BeaconStorageProvider createProvider(String type) {
        String normalizedType = type == null ? "YAML" : type.toUpperCase();

        return switch (normalizedType) {
            case "MYSQL" -> new MySqlBeaconStorageProvider(plugin);
            case "SQLITE" -> new SqliteBeaconStorageProvider(plugin);
            case "YAML" -> new YamlBeaconStorageProvider(plugin);
            default -> new YamlBeaconStorageProvider(plugin);
        };
    }

    public int migrateCount(String fromType, String toType) {
        BeaconStorageProvider fromProvider = createProvider(fromType);
        BeaconStorageProvider toProvider = createProvider(toType);

        try {
            List<BeaconData> beacons = fromProvider.loadBeacons();

            if (beacons.isEmpty()) {
                return 0;
            }

            for (BeaconData beacon : beacons) {
                toProvider.saveBeacon(beacon);
            }

            return beacons.size();
        } catch (Exception exception) {
            plugin.getLogger().severe(plugin.getLanguageManager().get(
                    "storage.migration-failed",
                    Map.of("error", exception.getMessage())));
            return -1;
        } finally {
            fromProvider.close();
            toProvider.close();
        }
    }

    public boolean isValidStorageType(String type) {
        if (type == null) {
            return false;
        }

        return type.equalsIgnoreCase("YAML")
                || type.equalsIgnoreCase("SQLITE")
                || type.equalsIgnoreCase("MYSQL");
    }

    public List<BeaconData> loadBeacons() {
        return provider.loadBeacons();
    }

    public void saveBeacon(BeaconData beacon) {
        provider.saveBeacon(beacon);
    }

    public void removeBeacon(String id) {
        provider.removeBeacon(id);
    }

    public void close() {
        provider.close();
    }

}