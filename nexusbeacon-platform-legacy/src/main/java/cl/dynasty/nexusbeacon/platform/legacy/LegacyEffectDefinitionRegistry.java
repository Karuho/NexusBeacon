package cl.dynasty.nexusbeacon.platform.legacy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import cl.dynasty.nexusbeacon.platform.api.MaterialContext;
import cl.dynasty.nexusbeacon.platform.api.PotionEffectResolution;

/** Validates configured effects once; unsupported gameplay definitions remain visible but cannot execute. */
public final class LegacyEffectDefinitionRegistry {
    private static final String[] EXECUTOR_TYPES = { "POTION", "CROP_BOOST", "SPAWNER_BOOST", "IGNITION",
            "DAMAGE_FIELD", "GRAVITY_PULSE", "BLOCK_PROCESS_BOOST" };
    private final Map<String, LegacyEffectDefinition> definitions;

    public LegacyEffectDefinitionRegistry(FileConfiguration config, LegacyMaterialResolver materials,
            LegacyPotionEffectResolver potions) {
        if (config == null || materials == null || potions == null) throw new NullPointerException();
        ConfigurationSection effects = config.getConfigurationSection("effects");
        if (effects == null) throw new IllegalArgumentException("effects.yml has no effects section");
        Map<String, LegacyEffectDefinition> parsed = new LinkedHashMap<String, LegacyEffectDefinition>();
        for (String rawId : effects.getKeys(false)) {
            ConfigurationSection section = effects.getConfigurationSection(rawId);
            if (section == null || !section.getBoolean("enabled", true)) continue;
            String id = rawId.toLowerCase(Locale.ROOT);
            String type = section.getString("type", "").toUpperCase(Locale.ROOT);
            if (!knownType(type)) throw new IllegalArgumentException("Unknown Legacy effect executor type: " + type);
            int maxLevel = section.getInt("max-level", 1);
            if (maxLevel < 1) throw new IllegalArgumentException("Invalid max-level for effect " + id);
            validateParameters(id, type, maxLevel, section);
            boolean supported = true;
            String diagnostic = "supported";
            org.bukkit.potion.PotionEffectType potion = null;
            if ("POTION".equals(type)) {
                PotionEffectResolution resolution = potions.resolvePotionEffect(section.getString("potion"));
                supported = resolution.isResolved();
                potion = resolution.getEffectType().orElse(null);
                if (!supported) diagnostic = "potion type is unavailable on this server";
            }
            List<Material> targetBlocks = new ArrayList<Material>();
            if ("BLOCK_PROCESS_BOOST".equals(type)) {
                List<String> targets = section.getStringList("target-blocks");
                if (targets.isEmpty()) targets = Collections.singletonList("FURNACE");
                for (String target : targets) {
                    LegacyMaterialResolution resolution = materials.resolveLegacyMaterial(target,
                            MaterialContext.BLOCK_MATCH);
                    if (!resolution.getResolution().isResolved()) {
                        supported = false;
                        diagnostic = "target block " + target + " is unavailable on this server";
                    } else {
                        targetBlocks.add(resolution.getResolution().getMaterial().get());
                    }
                }
            }
            LegacyEffectDefinition previous = parsed.put(id, new LegacyEffectDefinition(id, type, maxLevel,
                    section.getString("target", "PLAYERS"), potion,
                    section.getInt("duration-ticks", 100), section.getInt("amplifier-per-level", 1),
                    targetBlocks, supported, diagnostic));
            if (previous != null) throw new IllegalArgumentException("Duplicate effect id: " + id);
        }
        definitions = Collections.unmodifiableMap(parsed);
    }

    public LegacyEffectDefinition get(String id) {
        return id == null ? null : definitions.get(id.toLowerCase(Locale.ROOT));
    }
    public Collection<LegacyEffectDefinition> all() { return definitions.values(); }
    public int size() { return definitions.size(); }
    public int supportedCount() {
        int count = 0;
        for (LegacyEffectDefinition definition : definitions.values()) if (definition.isSupported()) count++;
        return count;
    }

    private static boolean knownType(String type) {
        for (String known : EXECUTOR_TYPES) if (known.equals(type)) return true;
        return false;
    }

    private static void validateParameters(String id, String type, int maxLevel, ConfigurationSection section) {
        if ("POTION".equals(type)) {
            if (section.getInt("duration-ticks", 100) <= 0 || section.getInt("amplifier-per-level", 1) <= 0) {
                invalid(id, "potion duration/amplifier must be positive");
            }
            String target = section.getString("target", "PLAYERS").toUpperCase(Locale.ROOT);
            if (!("PLAYERS".equals(target) || "MONSTERS".equals(target)
                    || "ANIMALS".equals(target) || "ALL_ENTITIES".equals(target))) {
                invalid(id, "unknown potion target " + target);
            }
        }
        for (int level = 1; level <= maxLevel; level++) {
            String prefix = "levels." + level + ".";
            if ("CROP_BOOST".equals(type)) {
                int chance = section.getInt(prefix + "growth-chance",
                        section.getInt("growth-chance-per-level", 15) * level);
                if (chance < 0 || chance > 100) invalid(id, "growth chance must be between 0 and 100");
            } else if ("IGNITION".equals(type)) {
                if (section.getInt(prefix + "fire-ticks", 60 * level) < 0) invalid(id, "fire ticks cannot be negative");
            } else if ("DAMAGE_FIELD".equals(type)) {
                if (section.getDouble(prefix + "damage", 1.0D * level) < 0.0D) invalid(id, "damage cannot be negative");
            } else if ("GRAVITY_PULSE".equals(type)) {
                if (section.getDouble(prefix + "pull-strength", 0.08D * level) < 0.0D
                        || section.getDouble(prefix + "max-velocity", 1.2D) <= 0.0D) {
                    invalid(id, "gravity strength/velocity is invalid");
                }
            } else if ("BLOCK_PROCESS_BOOST".equals(type)) {
                if (section.getInt(prefix + "speed-up-time", 8) < 0
                        || section.getInt(prefix + "fuel-speed-up-time", 8) < 0) {
                    invalid(id, "block process speed cannot be negative");
                }
            } else if ("SPAWNER_BOOST".equals(type)) {
                double percentage = section.getDouble(prefix + "speed-up-percentage", 15.0D * level);
                if (percentage < 0.0D || percentage > 95.0D
                        || section.getInt(prefix + "cooldown-ticks", 200) < 0) {
                    invalid(id, "spawner percentage/cooldown is invalid");
                }
            }
        }
    }

    private static void invalid(String id, String reason) {
        throw new IllegalArgumentException("Invalid Legacy effect " + id + ": " + reason);
    }
}
