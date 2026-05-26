package cl.dynasty.nexusbeacon.storage;

import java.util.List;

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
                .getString("storage.type", "YAML")
                .toUpperCase();

        switch (type) {
            case "MYSQL":
                plugin.getLogger().info("Storage activo: MYSQL");
                return new MySqlBeaconStorageProvider(plugin);

            case "SQLITE":
                plugin.getLogger().info("Storage activo: SQLITE");
                return new SqliteBeaconStorageProvider(plugin);

            case "YAML":
                plugin.getLogger().info("Storage activo: YAML");
                return new YamlBeaconStorageProvider(plugin);

            default:
                plugin.getLogger().warning("Storage desconocido: " + type + ". Usando YAML.");
                return new YamlBeaconStorageProvider(plugin);
        }
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