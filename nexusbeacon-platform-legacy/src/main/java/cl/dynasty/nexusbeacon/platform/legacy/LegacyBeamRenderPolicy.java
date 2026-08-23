package cl.dynasty.nexusbeacon.platform.legacy;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import cl.dynasty.nexusbeacon.platform.api.MaterialContext;

/** Reproduces the production custom-base predicate without changing beacon identity or power. */
public final class LegacyBeamRenderPolicy {
    private final RenderMode renderMode;
    private final int maxLayers;
    private final Set<Material> powerBlocks;

    public LegacyBeamRenderPolicy(ConfigurationSection beaconConfig, LegacyMaterialResolver materials) {
        if (beaconConfig == null || materials == null) throw new NullPointerException();
        this.renderMode = RenderMode.parse(beaconConfig.getString("visual-beam.render-mode", "AUTO"));
        this.maxLayers = Math.max(1, beaconConfig.getInt("beacon.power.max-layers", 4));
        Set<Material> configured = new HashSet<Material>();
        ConfigurationSection power = beaconConfig.getConfigurationSection("beacon.power");
        if (power != null) {
            addPowerBlocks(configured, power.getConfigurationSection("blocks"), materials);
            addPowerBlocks(configured, power.getConfigurationSection("vanilla-blocks"), materials);
            addPowerBlocks(configured, power.getConfigurationSection("custom-blocks"), materials);
        }
        this.powerBlocks = Collections.unmodifiableSet(configured);
    }

    LegacyBeamRenderPolicy(String renderMode, int maxLayers, Set<Material> powerBlocks) {
        this.renderMode = RenderMode.parse(renderMode);
        this.maxLayers = Math.max(1, maxLayers);
        this.powerBlocks = Collections.unmodifiableSet(new HashSet<Material>(powerBlocks));
    }

    public boolean shouldRender(World world, LegacyBeaconLocation beacon) {
        if (renderMode == RenderMode.ALWAYS) return true;
        if (world == null || beacon == null) return false;
        for (int layer = 1; layer <= maxLayers; layer++) {
            int y = beacon.getY() - layer;
            for (int x = -layer; x <= layer; x++) {
                for (int z = -layer; z <= layer; z++) {
                    Material material = world.getBlockAt(beacon.getX() + x, y, beacon.getZ() + z).getType();
                    if (powerBlocks.contains(material) && !isVanillaBeaconBase(material)) return true;
                }
            }
        }
        return false;
    }

    private static void addPowerBlocks(Set<Material> target, ConfigurationSection section,
            LegacyMaterialResolver materials) {
        if (section == null) return;
        for (String name : section.getKeys(false)) {
            if (section.getInt(name, 0) <= 0) continue;
            materials.resolveMaterial(name, MaterialContext.BLOCK_MATCH).getMaterial().ifPresent(target::add);
        }
    }

    private static boolean isVanillaBeaconBase(Material material) {
        if (material == null) return false;
        String name = material.name();
        return "IRON_BLOCK".equals(name) || "GOLD_BLOCK".equals(name)
                || "EMERALD_BLOCK".equals(name) || "DIAMOND_BLOCK".equals(name);
    }

    private enum RenderMode {
        ALWAYS, AUTO;

        private static RenderMode parse(String configured) {
            return configured != null && "ALWAYS".equalsIgnoreCase(configured) ? ALWAYS : AUTO;
        }
    }
}
