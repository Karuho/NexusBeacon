package cl.dynasty.nexusbeacon.effects.executor;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;

import cl.dynasty.nexusbeacon.NexusBeaconPlugin;
import cl.dynasty.nexusbeacon.effects.BeaconEffect;
import cl.dynasty.nexusbeacon.effects.EffectLevelUtil;
import cl.dynasty.nexusbeacon.model.BeaconData;
import cl.dynasty.nexusbeacon.util.DebugLogger;

public class CropBoostExecutor implements EffectExecutor {

    private final NexusBeaconPlugin plugin;
    private final Map<String, Integer> cursors = new HashMap<>();

    public CropBoostExecutor(NexusBeaconPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getType() {
        return "CROP_BOOST";
    }

    @Override
    public void tick(BeaconData beacon, BeaconEffect effect) {
        Location center = beacon.getLocation();

        if (center == null || center.getWorld() == null) {
            return;
        }

        int level = Math.max(1, beacon.getEffectLevel(effect.getId()));

        int radius = Math.min(
                beacon.getRange(),
                plugin.getConfigManager().getBeaconConfig()
                        .getInt("performance.crop-boost.scan-radius", beacon.getRange()));

        int verticalRadius = plugin.getConfigManager().getBeaconConfig()
                .getInt("performance.crop-boost.vertical-radius", 8);

        int maxBlocks = plugin.getConfigManager().getBeaconConfig()
                .getInt("performance.crop-boost.max-blocks-per-tick", 16);

        int maxScanned = plugin.getConfigManager().getBeaconConfig()
                .getInt("performance.crop-boost.max-scanned-blocks-per-tick", 2000);

        int chance = EffectLevelUtil.getLevelInt(
                plugin,
                effect,
                level,
                "growth-chance",
                plugin.getConfigManager().getEffectsConfig()
                        .getInt("effects." + effect.getId() + ".growth-chance-per-level", 15) * level);

        DebugLogger.log(plugin, effect.getType() + ":" + beacon.getId(), "EffectExecutor type=" + effect.getType()
                + " effect=" + effect.getId()
                + " level=" + level
                + " chance=" + chance
                + " radius=" + radius
                + " maxBlocks=" + maxBlocks
                + " maxScanned=" + maxScanned);

        int width = radius * 2 + 1;
        int total = width * width;

        String cursorKey = beacon.getId() + ":" + effect.getId();
        int cursor = cursors.getOrDefault(cursorKey, 0);

        int processed = 0;
        int scanned = 0;

        while (scanned < maxScanned && processed < maxBlocks && total > 0) {
            int index = cursor % total;

            int localZ = index % width;
            int localX = index / width;

            int x = localX - radius;
            int z = localZ - radius;

            cursor++;
            scanned++;

            if ((x * x) + (z * z) > radius * radius) {
                continue;
            }

            for (int y = -verticalRadius; y <= verticalRadius; y++) {
                Block block = center.getWorld().getBlockAt(
                        center.getBlockX() + x,
                        center.getBlockY() + y,
                        center.getBlockZ() + z);

                BlockData data = block.getBlockData();

                if (!(data instanceof Ageable)) {
                    continue;
                }

                Ageable ageable = (Ageable) data;

                if (ageable.getAge() >= ageable.getMaximumAge()) {
                    continue;
                }

                if (Math.random() * 100.0D > chance) {
                    continue;
                }

                ageable.setAge(ageable.getAge() + 1);
                block.setBlockData(ageable, true);

                processed++;
                break;
            }
        }

        cursors.put(cursorKey, cursor % total);

    }
}