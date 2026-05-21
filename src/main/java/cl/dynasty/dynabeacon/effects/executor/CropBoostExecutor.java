package cl.dynasty.dynabeacon.effects.executor;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.material.Crops;
import org.bukkit.material.MaterialData;

import cl.dynasty.dynabeacon.DynaBeaconPlugin;
import cl.dynasty.dynabeacon.effects.BeaconEffect;
import cl.dynasty.dynabeacon.model.BeaconData;

public class CropBoostExecutor implements EffectExecutor {

    private final DynaBeaconPlugin plugin;

    public CropBoostExecutor(DynaBeaconPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getType() {
        return "CROP_BOOST";
    }

    @Override
    public void tick(BeaconData beacon, BeaconEffect effect) {
        Location center = beacon.getLocation();
        if (center == null || center.getWorld() == null)
            return;

        int level = beacon.getEffectLevel(effect.getId());
        int radius = plugin.getConfigManager()
                .getEffectsConfig()
                .getInt("effects." + effect.getId() + ".scan-radius", Math.min(beacon.getRange(), 16));

        int maxBlocks = plugin.getConfigManager()
                .getEffectsConfig()
                .getInt("effects." + effect.getId() + ".max-blocks-per-tick", 32);

        int chance = plugin.getConfigManager()
                .getEffectsConfig()
                .getInt("effects." + effect.getId() + ".growth-chance-per-level", 15) * Math.max(1, level);

        int processed = 0;

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = -8; y <= 8; y++) {
                    if (processed >= maxBlocks)
                        return;

                    if (Math.random() * 100.0D > chance)
                        continue;

                    Block block = center.getWorld().getBlockAt(
                            center.getBlockX() + x,
                            center.getBlockY() + y,
                            center.getBlockZ() + z);

                    if (!isLegacyCrop(block))
                        continue;

                    BlockState state = block.getState();
                    MaterialData data = state.getData();

                    if (!(data instanceof Crops))
                        continue;

                    Crops crops = (Crops) data;

                    if (crops.getState() == org.bukkit.CropState.RIPE)
                        continue;

                    org.bukkit.CropState current = crops.getState();
                    org.bukkit.CropState[] states = org.bukkit.CropState.values();
                    int nextOrdinal = Math.min(current.ordinal() + 1, org.bukkit.CropState.RIPE.ordinal());

                    crops.setState(states[nextOrdinal]);
                    state.setData(crops);
                    state.update(true);

                    processed++;
                }
            }
        }
    }

    private boolean isLegacyCrop(Block block) {
        Material type = block.getType();

        return type == Material.CROPS
                || type == Material.CARROT
                || type == Material.POTATO
                || type == Material.NETHER_WARTS;
    }
}