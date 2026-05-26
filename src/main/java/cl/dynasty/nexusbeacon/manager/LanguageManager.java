package cl.dynasty.nexusbeacon.manager;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import cl.dynasty.nexusbeacon.NexusBeaconPlugin;

public final class LanguageManager {

    private final NexusBeaconPlugin plugin;
    private FileConfiguration languageConfig;
    private String prefix;

    public LanguageManager(NexusBeaconPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        String language = plugin.getConfigManager().getConfig().getString("language", "es_cl");
        String fileName = "languages/" + language + ".yml";

        File file = new File(plugin.getDataFolder(), fileName);

        if (!file.exists()) {
            plugin.saveResource(fileName, false);
        }

        languageConfig = YamlConfiguration.loadConfiguration(file);
        prefix = color(languageConfig.getString("prefix", "&b[NexusBeacon]&r "));
    }

    public String get(String path) {
        return get(path, Map.of());
    }

    public String get(String path, Map<String, String> placeholders) {
        String message = languageConfig.getString("messages." + path);

        if (message == null) {
            message = "&cMensaje no encontrado: " + path;
        }

        return apply(color(message), placeholders);
    }

    public List<String> getList(String path, Map<String, String> placeholders) {
        return languageConfig.getStringList(path)
                .stream()
                .map(this::color)
                .map(line -> apply(line, placeholders))
                .toList();
    }

    public String materialName(String key) {
        return languageConfig.getString("materials." + key, key);
    }

    public String withPrefix(String path) {
        return prefix + get(path);
    }

    public String withPrefix(String path, Map<String, String> placeholders) {
        return prefix + get(path, placeholders);
    }

    public String apply(String text, Map<String, String> placeholders) {
        String result = text;

        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace("%" + entry.getKey() + "%", entry.getValue());
        }

        return result;
    }

    public String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }
}