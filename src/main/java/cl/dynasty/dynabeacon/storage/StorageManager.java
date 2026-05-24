package cl.dynasty.dynabeacon.storage;

import java.util.List;

import cl.dynasty.dynabeacon.DynaBeaconPlugin;
import cl.dynasty.dynabeacon.model.BeaconData;

public class StorageManager {

    private final DynaBeaconPlugin plugin;
    private final BeaconStorageProvider provider;

    public StorageManager(DynaBeaconPlugin plugin) {
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
                plugin.getLogger().warning("Storage SQLITE aún no está implementado. Usando YAML.");
                return new YamlBeaconStorageProvider(plugin);

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