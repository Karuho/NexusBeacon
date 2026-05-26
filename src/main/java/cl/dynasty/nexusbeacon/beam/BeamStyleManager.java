package cl.dynasty.nexusbeacon.beam;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;

import cl.dynasty.nexusbeacon.NexusBeaconPlugin;

public final class BeamStyleManager {

    private final NexusBeaconPlugin plugin;
    private final Map<String, BeamStyle> styles = new HashMap<>();
    private BeamStyle globalStyle;

    public BeamStyleManager(NexusBeaconPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        styles.clear();

        ConfigurationSection stylesSection = plugin.getConfigManager()
                .getBeaconConfig()
                .getConfigurationSection("beam-styles");

        if (stylesSection != null) {
            for (String id : stylesSection.getKeys(false)) {
                ConfigurationSection section = stylesSection.getConfigurationSection(id);

                if (section == null) {
                    continue;
                }

                BeamStyle style = parseStyle(id, section);

                if (style != null) {
                    styles.put(id.toLowerCase(), style);
                }
            }
        }

        ConfigurationSection globalSection = plugin.getConfigManager()
                .getBeaconConfig()
                .getConfigurationSection("visual-beam.global-style");

        globalStyle = parseStyle("global", globalSection);

        if (globalStyle == null) {
            globalStyle = new BeamStyle(
                    "global",
                    "&bAqua",
                    Particle.DUST,
                    Color.AQUA,
                    1.2f,
                    "");
        }

        plugin.getLogger().info(plugin.getLanguageManager().get(
                "console.beam-styles-loaded",
                Map.of("amount", String.valueOf(styles.size()))));
    }

    public BeamStyle getStyle(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }

        return styles.get(id.toLowerCase());
    }

    public BeamStyle getGlobalStyle() {
        return globalStyle;
    }

    public Map<String, BeamStyle> getStyles() {
        return Collections.unmodifiableMap(styles);
    }

    private BeamStyle parseStyle(String id, ConfigurationSection section) {
        if (section == null) {
            return null;
        }

        String name = section.getString("name", id);
        Particle particle = parseParticle(section.getString("particle", "END_ROD"));
        Color color = parseColor(section.getString("color", "NONE"));
        float size = (float) section.getDouble("size", 1.0);
        String permission = section.getString("permission", "");

        return new BeamStyle(
                id,
                ChatColor.translateAlternateColorCodes('&', name),
                particle,
                color,
                size,
                permission);
    }

    private Particle parseParticle(String particleName) {
        if (particleName == null || particleName.isBlank()) {
            return Particle.END_ROD;
        }

        try {
            return Particle.valueOf(particleName.toUpperCase());
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning(plugin.getLanguageManager().get(
                    "console.invalid-particle",
                    Map.of("particle", particleName)));
            return Particle.END_ROD;
        }
    }

    private Color parseColor(String colorName) {
        if (colorName == null || colorName.equalsIgnoreCase("NONE")) {
            return null;
        }

        return switch (colorName.toUpperCase()) {
            case "WHITE" -> Color.WHITE;
            case "SILVER", "GRAY" -> Color.SILVER;
            case "BLACK" -> Color.BLACK;
            case "RED" -> Color.RED;
            case "MAROON" -> Color.MAROON;
            case "YELLOW" -> Color.YELLOW;
            case "OLIVE" -> Color.OLIVE;
            case "LIME", "GREEN" -> Color.LIME;
            case "AQUA", "CYAN" -> Color.AQUA;
            case "TEAL" -> Color.TEAL;
            case "BLUE" -> Color.BLUE;
            case "NAVY" -> Color.NAVY;
            case "FUCHSIA", "PURPLE", "MAGENTA" -> Color.FUCHSIA;
            case "ORANGE" -> Color.ORANGE;
            default -> {
                plugin.getLogger().warning(plugin.getLanguageManager().get(
                        "console.invalid-color",
                        Map.of("color", colorName)));
                yield null;
            }
        };
    }
}