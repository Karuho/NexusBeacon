package cl.dynasty.nexusbeacon.platform.legacy;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import cl.dynasty.nexusbeacon.platform.api.MaterialContext;

/** Validates real application metadata without constructing deferred gameplay systems. */
public final class LegacyApplicationConfigurationValidator {
    private static final Set<String> STORAGE_TYPES = new HashSet<String>(Arrays.asList("YAML", "SQLITE", "MYSQL"));
    private static final Set<String> RANGE_MODES = new HashSet<String>(Arrays.asList(
            "FIXED", "PERCENTILE", "EXPONENTIAL", "CLASSIFIED"));

    private final LegacyMaterialResolver materials;
    private final LegacyPotionEffectResolver potions;
    private final LegacyParticleService particles;
    private final LegacyBeamCompatibility beams;

    public LegacyApplicationConfigurationValidator(LegacyMaterialResolver materials,
            LegacyPotionEffectResolver potions, LegacyParticleService particles,
            LegacyBeamCompatibility beams) {
        if (materials == null) throw new NullPointerException("materials");
        if (potions == null) throw new NullPointerException("potions");
        if (particles == null) throw new NullPointerException("particles");
        if (beams == null) throw new NullPointerException("beams");
        this.materials = materials;
        this.potions = potions;
        this.particles = particles;
        this.beams = beams;
    }

    public LegacyApplicationConfiguration validate(FileConfiguration config, FileConfiguration beacon,
            FileConfiguration effects, FileConfiguration gui) {
        require(config, "config.yml");
        require(beacon, "beacon.yml");
        require(effects, "effects.yml");
        require(gui, "gui.yml");

        String language = required(config.getString("language"), "config.language");
        String storageType = required(config.getString("storage.type"), "config.storage.type").toUpperCase();
        if (!STORAGE_TYPES.contains(storageType)) {
            throw new IllegalArgumentException("Unsupported configured storage type: " + storageType);
        }

        requireMaterial(beacon.getString("item.material"), MaterialContext.REQUIRED_ITEM, "beacon.item.material");
        String rangeMode = required(beacon.getString("beacon.range-calculator.mode"),
                "beacon.range-calculator.mode").toUpperCase();
        if (!RANGE_MODES.contains(rangeMode)) {
            throw new IllegalArgumentException("Unsupported range calculator mode: " + rangeMode);
        }
        String rangeParticle = required(beacon.getString("beacon.particles.type"), "beacon.particles.type");
        if (!particles.resolve(rangeParticle).isSupported()) {
            throw new IllegalArgumentException("Unsupported range particle: " + rangeParticle);
        }

        int beamStyles = validateBeamStyles(beacon.getConfigurationSection("beam-styles"));
        EffectCounts effectCounts = validateEffects(effects.getConfigurationSection("effects"));
        GuiCounts guiCounts = validateGui(gui.getConfigurationSection("menus"));
        boolean recipeConfigured = validateRecipe(beacon.getConfigurationSection("recipe"));

        return new LegacyApplicationConfiguration(language, storageType, rangeMode,
                effectCounts.total, effectCounts.potions, effectCounts.deferred, effectCounts.unsupported,
                guiCounts.total, guiCounts.fallbacks, beamStyles, recipeConfigured);
    }

    private int validateBeamStyles(ConfigurationSection section) {
        if (section == null || section.getKeys(false).isEmpty()) {
            throw new IllegalArgumentException("beam-styles must define at least one style");
        }
        int count = 0;
        for (String id : section.getKeys(false)) {
            String particle = required(section.getString(id + ".particle"), "beam-styles." + id + ".particle");
            LegacyBeamStylePlan plan = new LegacyBeamStylePlan(id, particle,
                    beamColor(section.getString(id + ".color")),
                    (float) section.getDouble(id + ".size", 1.0D));
            if (beams.classify(plan).getStatus() == LegacyBeamCompatibilityStatus.UNSUPPORTED) {
                throw new IllegalArgumentException("Unsupported Legacy beam style: " + id);
            }
            count++;
        }
        return count;
    }

    private EffectCounts validateEffects(ConfigurationSection section) {
        if (section == null) throw new IllegalArgumentException("effects section is missing");
        EffectCounts counts = new EffectCounts();
        for (String id : section.getKeys(false)) {
            if (!section.getBoolean(id + ".enabled", true)) continue;
            String type = required(section.getString(id + ".type"), "effects." + id + ".type").toUpperCase();
            requireMaterial(section.getString(id + ".icon"), MaterialContext.GUI_ICON, "effects." + id + ".icon");
            counts.total++;
            if ("POTION".equals(type)) {
                String potion = required(section.getString(id + ".potion"), "effects." + id + ".potion");
                if (!potions.resolvePotionEffect(potion).isResolved()) {
                    counts.deferred++;
                    counts.unsupported++;
                    continue;
                }
                counts.potions++;
            } else {
                counts.deferred++;
            }
        }
        return counts;
    }

    private GuiCounts validateGui(ConfigurationSection menus) {
        if (menus == null) throw new IllegalArgumentException("GUI menus section is missing");
        GuiCounts counts = new GuiCounts();
        for (String menu : menus.getKeys(false)) {
            ConfigurationSection items = menus.getConfigurationSection(menu + ".items");
            if (items == null) continue;
            for (String symbol : items.getKeys(false)) {
                LegacyMaterialResolution result = materials.resolveLegacyMaterial(
                        required(items.getString(symbol + ".material"), "menus." + menu + ".items." + symbol),
                        MaterialContext.GUI_ICON);
                if (!result.getResolution().isResolved() && !result.getResolution().isFallbackUsed()) {
                    throw new IllegalArgumentException("GUI material cannot be represented: " + menu + "/" + symbol);
                }
                counts.total++;
                if (result.getResolution().isFallbackUsed()) counts.fallbacks++;
            }
        }
        return counts;
    }

    private boolean validateRecipe(ConfigurationSection recipe) {
        if (recipe == null || !recipe.getBoolean("enabled", false)) return false;
        // Productive recipe validation is isolated so an invalid recipe cannot disable storage/gameplay.
        return true;
    }

    private void requireMaterial(String id, MaterialContext context, String path) {
        cl.dynasty.nexusbeacon.platform.api.MaterialResolution resolution =
                materials.resolveMaterial(required(id, path), context);
        if (!resolution.isResolved() && !resolution.isFallbackUsed()) {
            throw new IllegalArgumentException("Unsupported material at " + path + ": " + id);
        }
    }

    private static LegacyParticleColor beamColor(String name) {
        if (name == null || name.trim().isEmpty() || "NONE".equalsIgnoreCase(name.trim())) return null;
        String normalized = name.trim().toUpperCase();
        if ("AQUA".equals(normalized)) return new LegacyParticleColor(0, 255, 255);
        if ("RED".equals(normalized)) return new LegacyParticleColor(255, 0, 0);
        if ("LIME".equals(normalized) || "GREEN".equals(normalized)) {
            return new LegacyParticleColor(0, 255, 0);
        }
        if ("PURPLE".equals(normalized)) return new LegacyParticleColor(255, 0, 255);
        throw new IllegalArgumentException("Unsupported configured beam color: " + name);
    }

    private static void require(Object value, String name) {
        if (value == null) throw new NullPointerException(name);
    }

    private static String required(String value, String path) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Missing required configuration value: " + path);
        }
        return value.trim();
    }

    private static final class EffectCounts {
        private int total;
        private int potions;
        private int deferred;
        private int unsupported;
    }
    private static final class GuiCounts { private int total; private int fallbacks; }
}
