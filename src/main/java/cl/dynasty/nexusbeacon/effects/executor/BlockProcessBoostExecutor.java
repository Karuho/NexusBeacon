package cl.dynasty.nexusbeacon.effects.executor;

import java.lang.reflect.Method;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Furnace;
import org.bukkit.configuration.ConfigurationSection;

import cl.dynasty.nexusbeacon.NexusBeaconPlugin;
import cl.dynasty.nexusbeacon.effects.BeaconEffect;
import cl.dynasty.nexusbeacon.model.BeaconData;

public class BlockProcessBoostExecutor implements EffectExecutor {

    private final NexusBeaconPlugin plugin;

    public BlockProcessBoostExecutor(NexusBeaconPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getType() {
        return "BLOCK_PROCESS_BOOST";
    }

    @Override
    public void tick(BeaconData beacon, BeaconEffect effect) {
        Location center = beacon.getLocation();
        if (center == null || center.getWorld() == null)
            return;

        ConfigurationSection section = plugin.getConfigManager()
                .getEffectsConfig()
                .getConfigurationSection("effects." + effect.getId());

        if (section == null)
            return;

        int level = Math.max(1, beacon.getEffectLevel(effect.getId()));
        int radius = Math.min(
        beacon.getRange(),
        section.getInt("scan-radius", beacon.getRange()));
        int verticalRadius = section.getInt("vertical-radius", radius);
        int maxBlocksPerTick = section.getInt("max-blocks-per-tick", 32);
        int maxScannedBlocks = section.getInt("max-scanned-blocks-per-tick", 2000);

        List<String> targetBlocks = section.getStringList("target-blocks");
        if (targetBlocks.isEmpty()) {
            targetBlocks = java.util.Arrays.asList("FURNACE");
        }

        int scanned = 0;
        int processed = 0;

        for (int x = -radius; x <= radius; x++) {
            for (int y = -verticalRadius; y <= verticalRadius; y++) {
                for (int z = -radius; z <= radius; z++) {

                    if (processed >= maxBlocksPerTick)
                        return;

                    if (++scanned >= maxScannedBlocks)
                        return;

                    Block block = center.getWorld().getBlockAt(
                            center.getBlockX() + x,
                            center.getBlockY() + y,
                            center.getBlockZ() + z);

                    if (!isTargetBlock(block.getType(), targetBlocks))
                        continue;

                    if (!(block.getState() instanceof Furnace))
                        continue;

                    Furnace furnace = (Furnace) block.getState();

                    if (furnace.getBurnTime() <= 0)
                        continue;

                    int speedUpTime = section.getInt(
                            "levels." + level + ".speed-up-time",
                            section.getInt("speed-up-time", 8));

                    int fuelSpeedUpTime = section.getInt(
                            "levels." + level + ".fuel-speed-up-time",
                            section.getInt("fuel-speed-up-time", speedUpTime));

                    int currentTotal = getCookTimeTotal(furnace);

                    if (currentTotal <= 0) {
                        currentTotal = isModernFurnace(block.getType()) ? 100 : 200;
                    }

                    short currentCook = furnace.getCookTime();
                    short newCook = (short) Math.min(currentTotal - 1, currentCook + speedUpTime);
                    furnace.setCookTime(newCook);

                    if (furnace.getBurnTime() < fuelSpeedUpTime) {
                        furnace.setBurnTime((short) fuelSpeedUpTime);
                    }

                    furnace.update(true);
                    processed++;
                }
            }
        }
    }

    private int getCookTimeTotal(Furnace furnace) {
        try {
            Method method = furnace.getClass().getMethod("getCookTimeTotal");
            Object result = method.invoke(furnace);
            return result instanceof Number ? ((Number) result).intValue() : -1;
        } catch (Exception e) {
            return -1;
        }
    }

    private boolean isModernFurnace(Material material) {
        String name = material.name();
        return name.equals("BLAST_FURNACE") || name.equals("SMOKER");
    }

    private boolean isTargetBlock(Material material, List<String> targetBlocks) {
        for (String target : targetBlocks) {
            Material targetMaterial = plugin.getVersionAdapter().material(target);
            if (targetMaterial == material)
                return true;
        }
        return false;
    }
}