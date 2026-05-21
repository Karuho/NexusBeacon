package cl.dynasty.dynabeacon.manager;

import cl.dynasty.dynabeacon.DynaBeaconPlugin;
import cl.dynasty.dynabeacon.model.PlayerSettings;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerSettingsManager {

    private final DynaBeaconPlugin plugin;
    private final Map<UUID, PlayerSettings> cache = new HashMap<UUID, PlayerSettings>();

    public PlayerSettingsManager(DynaBeaconPlugin plugin) {
        this.plugin = plugin;
    }

    public PlayerSettings get(UUID uuid) {
        if (cache.containsKey(uuid)) {
            return cache.get(uuid);
        }

        PlayerSettings settings = load(uuid);
        cache.put(uuid, settings);
        return settings;
    }

    public PlayerSettings load(UUID uuid) {
        File file = getPlayerFile(uuid);

        if (!file.exists()) {
            PlayerSettings settings = new PlayerSettings(uuid);
            save(settings);
            return settings;
        }

        FileConfiguration yaml = YamlConfiguration.loadConfiguration(file);

        return new PlayerSettings(
        uuid,
        yaml.getBoolean("showParticle", true),
        yaml.getBoolean("showAnimation", true),
        yaml.getStringList("beaconInfoList"),
        yaml.getString("particleType", "VILLAGER_HAPPY")
);
    }

    public void save(PlayerSettings settings) {
        File file = getPlayerFile(settings.getUuid());
        File folder = file.getParentFile();

        if (!folder.exists()) {
            folder.mkdirs();
        }

        FileConfiguration yaml = new YamlConfiguration();

        yaml.set("showParticle", settings.isShowParticle());
        yaml.set("showAnimation", settings.isShowAnimation());
        yaml.set("beaconInfoList", settings.getBeaconInfoList());
        yaml.set("particleType", settings.getParticleType());

        try {
            yaml.save(file);
        } catch (Exception exception) {
            plugin.getLogger().severe("No se pudo guardar el archivo del jugador " + settings.getUuid());
            exception.printStackTrace();
        }
    }

    private File getPlayerFile(UUID uuid) {
        return new File(plugin.getDataFolder(), "players/" + uuid.toString() + ".yml");
    }
}