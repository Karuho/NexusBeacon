package cl.dynasty.nexusbeacon.effects;

import org.bukkit.configuration.ConfigurationSection;

import cl.dynasty.nexusbeacon.NexusBeaconPlugin;

public final class EffectLevelUtil {

    private EffectLevelUtil() {
    }

    public static ConfigurationSection getEffectSection(NexusBeaconPlugin plugin, BeaconEffect effect) {
        return plugin.getConfigManager()
                .getEffectsConfig()
                .getConfigurationSection("effects." + effect.getId());
    }

    public static ConfigurationSection getLevelSection(NexusBeaconPlugin plugin, BeaconEffect effect, int level) {
        ConfigurationSection effectSection = getEffectSection(plugin, effect);

        if (effectSection == null) {
            return null;
        }

        return effectSection.getConfigurationSection("levels." + level);
    }

    public static boolean isLevelEnabled(NexusBeaconPlugin plugin, BeaconEffect effect, int level) {
        ConfigurationSection levelSection = getLevelSection(plugin, effect, level);

        if (levelSection == null) {
            return true;
        }

        return levelSection.getBoolean("enabled", true);
    }

    public static int getLevelInt(NexusBeaconPlugin plugin, BeaconEffect effect, int level, String key, int fallback) {
        ConfigurationSection levelSection = getLevelSection(plugin, effect, level);

        if (levelSection != null && levelSection.contains(key)) {
            return levelSection.getInt(key, fallback);
        }

        ConfigurationSection effectSection = getEffectSection(plugin, effect);

        if (effectSection != null) {
            return effectSection.getInt(key, fallback);
        }

        return fallback;
    }

    public static double getLevelDouble(NexusBeaconPlugin plugin, BeaconEffect effect, int level, String key, double fallback) {
        ConfigurationSection levelSection = getLevelSection(plugin, effect, level);

        if (levelSection != null && levelSection.contains(key)) {
            return levelSection.getDouble(key, fallback);
        }

        ConfigurationSection effectSection = getEffectSection(plugin, effect);

        if (effectSection != null) {
            return effectSection.getDouble(key, fallback);
        }

        return fallback;
    }
}