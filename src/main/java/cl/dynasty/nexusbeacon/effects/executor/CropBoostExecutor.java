package cl.dynasty.nexusbeacon.effects.executor;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;

import cl.dynasty.nexusbeacon.NexusBeaconPlugin;
import cl.dynasty.nexusbeacon.effects.BeaconEffect;
import cl.dynasty.nexusbeacon.effects.EffectLevelUtil;
import cl.dynasty.nexusbeacon.model.BeaconData;

public class CropBoostExecutor implements EffectExecutor {

    private final NexusBeaconPlugin plugin;

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

        int radius = plugin.getConfigManager()
                .getBeaconConfig()
                .getInt("performance.crop-boost.scan-radius", 16);

        int verticalRadius = plugin.getConfigManager()
                .getBeaconConfig()
                .getInt("performance.crop-boost.vertical-radius", 8);

        int maxBlocks = plugin.getConfigManager()
                .getBeaconConfig()
                .getInt("performance.crop-boost.max-blocks-per-tick", 16);

        int chance = EffectLevelUtil.getLevelInt(
                plugin,
                effect,
                level,
                "growth-chance",
                plugin.getConfigManager()
                        .getEffectsConfig()
                        .getInt("effects." + effect.getId() + ".growth-chance-per-level", 15) * level);

        int processed = 0;

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = -verticalRadius; y <= verticalRadius; y++) {
                    if (processed >= maxBlocks) {
                        return;
                    }

                    if (Math.random() * 100.0D > chance) {
                        continue;
                    }

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

                    ageable.setAge(ageable.getAge() + 1);
                    block.setBlockData(ageable, true);

                    processed++;
                }
            }
        }
    }
}