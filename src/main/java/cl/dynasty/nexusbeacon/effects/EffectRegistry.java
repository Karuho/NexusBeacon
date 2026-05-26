package cl.dynasty.nexusbeacon.effects;

import cl.dynasty.nexusbeacon.NexusBeaconPlugin;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.potion.PotionEffectType;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EffectRegistry {

    private final NexusBeaconPlugin plugin;
    private final Map<String, BeaconEffect> effects = new HashMap<String, BeaconEffect>();

    public EffectRegistry(NexusBeaconPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        effects.clear();

        ConfigurationSection section = plugin.getConfigManager().getEffectsConfig().getConfigurationSection("effects");
        if (section == null)
            return;

        for (String id : section.getKeys(false)) {
            if (!section.getBoolean(id + ".enabled", true))
                continue;

            String type = section.getString(id + ".type", "POTION");

            String displayName = section.getString(id + ".display-name", id);
            List<String> description = section.getStringList(id + ".description");

            String iconName = section.getString(id + ".icon", "BEACON");
            Material icon = plugin.getVersionAdapter().material(iconName);

            int durationTicks = section.getInt(id + ".duration-ticks", 100);
            int maxLevel = section.getInt(id + ".max-level", 1);
            int powerConsumption = section.getInt(id + ".power-consumption", 1);

            if (type.equalsIgnoreCase("POTION")) {
                String potionName = section.getString(id + ".potion", "");
                PotionEffectType potionType = plugin.getVersionAdapter().potion(potionName);

                if (potionType == null) {
                    plugin.getLogger().warning("Poción inválida para efecto: " + id);
                    continue;
                }

                String target = section.getString(id + ".target", "PLAYERS");
                int amplifierPerLevel = section.getInt(id + ".amplifier-per-level", 1);

                effects.put(id.toLowerCase(), new PotionBeaconEffect(
                        id.toLowerCase(),
                        displayName,
                        description,
                        icon,
                        potionType,
                        target,
                        amplifierPerLevel,
                        durationTicks,
                        maxLevel,
                        powerConsumption));

                continue;
            }

            effects.put(id.toLowerCase(), new ConfiguredBeaconEffect(
                    id.toLowerCase(),
                    type.toUpperCase(),
                    displayName,
                    description,
                    icon,
                    maxLevel,
                    powerConsumption));
        }

        plugin.getLogger().info("Efectos cargados: " + effects.size());
    }

    public BeaconEffect getEffect(String id) {
        if (id == null)
            return null;
        return effects.get(id.toLowerCase());
    }

    public Collection<BeaconEffect> getEffects() {
        return effects.values();
    }
}