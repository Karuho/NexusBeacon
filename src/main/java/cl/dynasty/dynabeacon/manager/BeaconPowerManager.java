package cl.dynasty.dynabeacon.manager;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;

import cl.dynasty.dynabeacon.DynaBeaconPlugin;
import cl.dynasty.dynabeacon.effects.BeaconEffect;
import cl.dynasty.dynabeacon.model.BeaconData;
import cl.dynasty.dynabeacon.range.RangeCalculator;
import cl.dynasty.dynabeacon.range.RangeCalculatorFactory;

public class BeaconPowerManager {

    private final DynaBeaconPlugin plugin;

    public BeaconPowerManager(DynaBeaconPlugin plugin) {
        this.plugin = plugin;
    }

    public int getAvailablePower(BeaconData beacon) {
        if (beacon == null || beacon.getLocation() == null)
            return 0;

        Location location = beacon.getLocation();
        int maxLayers = plugin.getConfigManager().getBeaconConfig().getInt("beacon.power.max-layers", 4);

        int power = 0;

        for (int layer = 1; layer <= maxLayers; layer++) {
            int y = location.getBlockY() - layer;

            for (int x = -layer; x <= layer; x++) {
                for (int z = -layer; z <= layer; z++) {
                    Block block = location.getWorld().getBlockAt(
                            location.getBlockX() + x,
                            y,
                            location.getBlockZ() + z);

                    power += getBlockPower(block.getType());
                }
            }
        }

        return power;
    }

    public int getUsedPower(BeaconData beacon) {
        int used = 0;

        for (String effectId : beacon.getActiveEffects()) {
            BeaconEffect effect = plugin.getEffectRegistry().getEffect(effectId);

            if (effect != null) {
                int level = Math.max(1, beacon.getEffectLevel(effectId));
                used += cl.dynasty.dynabeacon.effects.EffectLevelUtil.getLevelInt(
                        plugin,
                        effect,
                        level,
                        "power-consumption",
                        effect.getPowerConsumption() * level);
            }
        }

        return used;
    }

    public boolean canActivate(BeaconData beacon, BeaconEffect effect) {
        int available = getAvailablePower(beacon);
        int used = getUsedPower(beacon);

        int level = Math.max(1, beacon.getEffectLevel(effect.getId()));
        int needed = cl.dynasty.dynabeacon.effects.EffectLevelUtil.getLevelInt(
                plugin,
                effect,
                level,
                "power-consumption",
                effect.getPowerConsumption() * level);

        if (beacon.isEffectActive(effect.getId())) {
            used -= needed;
        }

        return used + needed <= available;
    }

    public int calculateRange(BeaconData beacon) {
        int fallbackRange = plugin.getConfigManager()
                .getBeaconConfig()
                .getInt("beacon.default-range", 100);

        int maxRange = plugin.getConfigManager()
                .getBeaconConfig()
                .getInt("beacon.max-range", fallbackRange);

        int power = getAvailablePower(beacon);

        RangeCalculator calculator = RangeCalculatorFactory.create(
                plugin.getConfigManager()
                        .getBeaconConfig()
                        .getConfigurationSection("beacon.range-calculator"),
                fallbackRange);

        return Math.min(calculator.calculate(power), maxRange);
    }

    public BeaconData getBeaconByBaseBlock(Block block) {
        if (block == null || block.getWorld() == null)
            return null;

        int maxLayers = plugin.getConfigManager().getBeaconConfig().getInt("beacon.power.max-layers", 4);

        for (BeaconData beacon : plugin.getBeaconManager().getBeacons()) {
            Location beaconLocation = beacon.getLocation();

            if (beaconLocation == null || beaconLocation.getWorld() == null)
                continue;
            if (!beaconLocation.getWorld().equals(block.getWorld()))
                continue;
            if (!beacon.isProtectBaseBlocks())
                continue;

            for (int layer = 1; layer <= maxLayers; layer++) {
                int y = beaconLocation.getBlockY() - layer;

                if (block.getY() != y)
                    continue;

                int dx = Math.abs(block.getX() - beaconLocation.getBlockX());
                int dz = Math.abs(block.getZ() - beaconLocation.getBlockZ());

                if (dx <= layer && dz <= layer) {
                    return beacon;
                }
            }
        }

        return null;
    }

    private int getBlockPower(Material material) {
        ConfigurationSection section = plugin.getConfigManager()
                .getBeaconConfig()
                .getConfigurationSection("beacon.power.blocks");

        if (section == null || material == null)
            return 0;

        return section.getInt(material.name(), 0);
    }

}