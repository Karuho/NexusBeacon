package cl.dynasty.dynabeacon.effects.executor;

import java.lang.reflect.Method;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Furnace;
import org.bukkit.configuration.ConfigurationSection;

import cl.dynasty.dynabeacon.DynaBeaconPlugin;
import cl.dynasty.dynabeacon.effects.BeaconEffect;
import cl.dynasty.dynabeacon.model.BeaconData;

public class BlockProcessBoostExecutor implements EffectExecutor {

    private final DynaBeaconPlugin plugin;

    public BlockProcessBoostExecutor(DynaBeaconPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getType() {
        return "BLOCK_PROCESS_BOOST";
    }

    @Override
    public void tick(BeaconData beacon, BeaconEffect effect) {
        Location center = beacon.getLocation();

        if (center == null || center.getWorld() == null) {
            return;
        }

        ConfigurationSection section = plugin.getConfigManager()
                .getEffectsConfig()
                .getConfigurationSection("effects." + effect.getId());

        if (section == null) {
            return;
        }

        int level = Math.max(1, beacon.getEffectLevel(effect.getId()));
        int radius = section.getInt("scan-radius", Math.min(beacon.getRange(), 16));
        int verticalRadius = section.getInt("vertical-radius", radius);
        int maxBlocksPerTick = section.getInt("max-blocks-per-tick", 32);

        int processTimeLevel1 = section.getInt("process-time-level-1", 120);
        int processTimeLevel2 = section.getInt("process-time-level-2", 80);
        int processTimeLevel3 = section.getInt("process-time-level-3", 40);

        int wantedTotalTime = processTimeLevel1;

        if (level >= 2) {
            wantedTotalTime = processTimeLevel2;
        }

        if (level >= 3) {
            wantedTotalTime = processTimeLevel3;
        }

        java.util.List<String> targetBlocks = section.getStringList("target-blocks");

        if (targetBlocks.isEmpty()) {
            targetBlocks.add("FURNACE");
        }

        int processed = 0;

        for (int x = -radius; x <= radius; x++) {
            for (int y = -verticalRadius; y <= verticalRadius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if (processed >= maxBlocksPerTick) {
                        return;
                    }

                    Block block = center.getWorld().getBlockAt(
                            center.getBlockX() + x,
                            center.getBlockY() + y,
                            center.getBlockZ() + z);

                    if (!isTargetBlock(block.getType(), targetBlocks)) {
                        continue;
                    }

                    if (!(block.getState() instanceof Furnace)) {
                        continue;
                    }

                    Furnace furnace = (Furnace) block.getState();
                    plugin.getLogger().info("[DynaBeacon DEBUG] Furnace encontrado: "
                            + block.getType()
                            + " cook=" + furnace.getCookTime()
                            + " burn=" + furnace.getBurnTime()
                            + " level=" + level);

                    if (furnace.getBurnTime() <= 0 && furnace.getCookTime() <= 0) {
                        continue;
                    }
                    setCookTimeTotalIfSupported(furnace, wantedTotalTime);

                    short currentCookTime = furnace.getCookTime();

                    if (currentCookTime < wantedTotalTime - 2) {
                        furnace.setCookTime((short) Math.min(wantedTotalTime - 2, currentCookTime + 20));
                    }

                    if (furnace.getBurnTime() < 40) {
                        furnace.setBurnTime((short) 40);
                    }

                    furnace.update(true);
                    plugin.getLogger().info("[DynaBeacon DEBUG] Furnace actualizado: "
                            + block.getType()
                            + " cook=" + furnace.getCookTime()
                            + " burn=" + furnace.getBurnTime());
                    processed++;
                }
            }
        }
    }

    private void setCookTimeTotalIfSupported(Furnace furnace, int totalTime) {
        try {
            Method method = furnace.getClass().getMethod("setCookTimeTotal", int.class);
            method.invoke(furnace, totalTime);
        } catch (Exception ignored) {
        }
    }

    private boolean isTargetBlock(Material material, java.util.List<String> targetBlocks) {
        for (String target : targetBlocks) {
            Material targetMaterial = plugin.getVersionAdapter().material(target, target);

            if (targetMaterial == material) {
                return true;
            }
        }

        return false;
    }
}