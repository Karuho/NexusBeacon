package cl.dynasty.dynabeacon.config;

import java.io.File;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import cl.dynasty.dynabeacon.DynaBeaconPlugin;

public class ConfigManager {

    private final DynaBeaconPlugin plugin;

    private FileConfiguration config;
    private FileConfiguration beaconConfig;
    private FileConfiguration effectsConfig;
    private FileConfiguration guiConfig;
    private FileConfiguration storageConfig;
    private FileConfiguration languageConfig;

    private File storageFile;

    public ConfigManager(DynaBeaconPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        saveResourceIfMissing("beacon.yml");
        saveResourceIfMissing("effects.yml");
        saveResourceIfMissing("gui.yml");
        saveResourceIfMissing("storage.yml");
        saveResourceIfMissing("languages/es_cl.yml");

        plugin.reloadConfig();
        config = plugin.getConfig();

        beaconConfig = loadFile("beacon.yml");
        effectsConfig = loadFile("effects.yml");
        guiConfig = loadFile("gui.yml");

        storageFile = new File(plugin.getDataFolder(), "storage.yml");
        storageConfig = YamlConfiguration.loadConfiguration(storageFile);

        String language = config.getString("language", "es_cl");
        languageConfig = loadFile("languages/" + language + ".yml");
    }

    private FileConfiguration loadFile(String path) {
        File file = new File(plugin.getDataFolder(), path);
        return YamlConfiguration.loadConfiguration(file);
    }

    public void saveStorage() {
        try {
            storageConfig.save(storageFile);
        } catch (Exception exception) {
            plugin.getLogger().severe("No se pudo guardar storage.yml");
            exception.printStackTrace();
        }
    }

    private void saveResourceIfMissing(String path) {
    java.io.File file = new java.io.File(plugin.getDataFolder(), path);

    if (!file.exists()) {
        plugin.saveResource(path, false);
    }
}

    public FileConfiguration getConfig() {
        return config;
    }

    public FileConfiguration getBeaconConfig() {
        return beaconConfig;
    }

    public FileConfiguration getEffectsConfig() {
        return effectsConfig;
    }

    public FileConfiguration getGuiConfig() {
        return guiConfig;
    }

    public FileConfiguration getStorageConfig() {
        return storageConfig;
    }

    public FileConfiguration getLanguageConfig() {
        return languageConfig;
    }
}