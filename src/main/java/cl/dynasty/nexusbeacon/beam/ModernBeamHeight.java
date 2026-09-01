package cl.dynasty.nexusbeacon.beam;

import org.bukkit.configuration.ConfigurationSection;

final class ModernBeamHeight {
    static final String FIXED = "FIXED";
    static final String WORLD_MAX = "WORLD_MAX";

    private ModernBeamHeight() {}

    static String mode(ConfigurationSection config) {
        String configured = config.getString("visual-beam.height-mode");
        return WORLD_MAX.equalsIgnoreCase(configured) ? WORLD_MAX : FIXED;
    }

    static int height(String mode, int fixedHeight, int beaconY, int worldMaximumHeight) {
        return WORLD_MAX.equals(mode)
                ? Math.max(1, worldMaximumHeight - (beaconY + 1))
                : Math.max(1, fixedHeight);
    }
}
