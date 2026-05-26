package cl.dynasty.nexusbeacon.storage;

import java.util.List;
import java.util.Map;

import cl.dynasty.nexusbeacon.NexusBeaconPlugin;
import cl.dynasty.nexusbeacon.model.BeaconData;

public class StorageManager {

    private final NexusBeaconPlugin plugin;
    private final BeaconStorageProvider provider;
    private static final String STORAGE_ACTIVE = "console.storage-active";

    public StorageManager(NexusBeaconPlugin plugin) {
        this.plugin = plugin;
        this.provider = createProvider();
    }

    private BeaconStorageProvider createProvider() {
        String type = plugin.getConfigManager()
                .getConfig()
                .getString("storage.type", "YAML")
                .toUpperCase();

        return switch (type) {
            case "MYSQL" -> {
                plugin.getLanguageManager().get(
                        STORAGE_ACTIVE,
                        Map.of("type", "MYSQL"));
                yield new MySqlBeaconStorageProvider(plugin);
            }
            case "SQLITE" -> {
                plugin.getLanguageManager().get(
                        STORAGE_ACTIVE,
                        Map.of("type", "SQLITE"));
                yield new SqliteBeaconStorageProvider(plugin);
            }
            case "YAML" -> {
                plugin.getLanguageManager().get(
                        STORAGE_ACTIVE,
                        Map.of("type", "YAML"));
                yield new YamlBeaconStorageProvider(plugin);
            }
            default -> {
                plugin.getLanguageManager().get(
                        STORAGE_ACTIVE,
                        Map.of("type", type));
                yield new YamlBeaconStorageProvider(plugin);
            }
        };
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