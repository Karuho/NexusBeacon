package cl.dynasty.dynabeacon.effects;

import org.bukkit.configuration.ConfigurationSection;

import cl.dynasty.dynabeacon.DynaBeaconPlugin;

public final class EffectLevelUtil {

    private EffectLevelUtil() {
    }

    public static ConfigurationSection getEffectSection(DynaBeaconPlugin plugin, BeaconEffect effect) {
        return plugin.getConfigManager()
                .getEffectsConfig()
                .getConfigurationSection("effects." + effect.getId());
    }

    public static ConfigurationSection getLevelSection(DynaBeaconPlugin plugin, BeaconEffect effect, int level) {
        ConfigurationSection effectSection = getEffectSection(plugin, effect);

        if (effectSection == null) {
            return null;
        }

        return effectSection.getConfigurationSection("levels." + level);
    }

    public static boolean isLevelEnabled(DynaBeaconPlugin plugin, BeaconEffect effect, int level) {
        ConfigurationSection levelSection = getLevelSection(plugin, effect, level);

        if (levelSection == null) {
            return true;
        }

        return levelSection.getBoolean("enabled", true);
    }

    public static int getLevelInt(DynaBeaconPlugin plugin, BeaconEffect effect, int level, String key, int fallback) {
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

    public static double getLevelDouble(DynaBeaconPlugin plugin, BeaconEffect effect, int level, String key, double fallback) {
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