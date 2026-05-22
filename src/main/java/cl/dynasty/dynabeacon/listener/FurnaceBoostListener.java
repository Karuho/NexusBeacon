package cl.dynasty.dynabeacon.listener;

import cl.dynasty.dynabeacon.DynaBeaconPlugin;
import cl.dynasty.dynabeacon.effects.BeaconEffect;
import cl.dynasty.dynabeacon.model.BeaconData;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Furnace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;

public class FurnaceBoostListener implements Listener {

    private final DynaBeaconPlugin plugin;

    public FurnaceBoostListener(DynaBeaconPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onFurnaceBurn(FurnaceBurnEvent event) {
        FurnaceBoostData boost = getBestBoost(event.getBlock());
        if (boost == null) return;

        int reduction = (int) (event.getBurnTime() * (boost.fuelBoostPercent / 100.0D));
        int newBurnTime = Math.max(1, event.getBurnTime() - reduction);

        event.setBurnTime(newBurnTime);
    }

    @EventHandler(ignoreCancelled = true)
    public void onFurnaceSmelt(FurnaceSmeltEvent event) {
        FurnaceBoostData boost = getBestBoost(event.getBlock());
        if (boost == null) return;

        Block block = event.getBlock();

        plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                if (!(block.getState() instanceof Furnace)) return;

                Furnace furnace = (Furnace) block.getState();

                if (furnace.getInventory().getSmelting() == null) return;

                int total = furnace.getCookTimeTotal();
                if (total <= 0) total = 200;

                int add = (int) (total * (boost.cookBoostPercent / 100.0D));
                int next = Math.min(total, furnace.getCookTime() + add);

                furnace.setCookTime((short) next);
                furnace.update(true);
            }
        });
    }

    private FurnaceBoostData getBestBoost(Block block) {
        FurnaceBoostData best = null;

        for (BeaconData beacon : plugin.getBeaconManager().getBeacons()) {
            if (beacon.getLocation() == null || beacon.getLocation().getWorld() == null) continue;
            if (!beacon.getLocation().getWorld().equals(block.getWorld())) continue;

            if (!isInInfiniteCylinder(beacon, block)) continue;

            for (String effectId : beacon.getActiveEffects()) {
                BeaconEffect effect = plugin.getEffectRegistry().getEffect(effectId);
                if (effect == null) continue;
                if (!effect.getType().equalsIgnoreCase("BLOCK_PROCESS_BOOST")) continue;

                ConfigurationSection section = plugin.getConfigManager()
                        .getEffectsConfig()
                        .getConfigurationSection("effects." + effect.getId());

                if (section == null) continue;
                if (!isTargetBlock(block.getType(), section)) continue;

                int level = Math.max(1, beacon.getEffectLevel(effect.getId()));

                double cookPercent = section.getDouble("speed-up-time-per-level", 15.0D) * level;
                double fuelPercent = section.getDouble("fuel-speed-up-time-per-level", cookPercent) * level;

                FurnaceBoostData data = new FurnaceBoostData(cookPercent, fuelPercent);

                if (best == null || data.cookBoostPercent > best.cookBoostPercent) {
                    best = data;
                }
            }
        }

        return best;
    }

    private boolean isTargetBlock(Material material, ConfigurationSection section) {
        java.util.List<String> targets = section.getStringList("target-blocks");

        if (targets.isEmpty()) {
            targets.add("FURNACE");
        }

        for (String target : targets) {
            Material targetMaterial = plugin.getVersionAdapter().material(target);

            if (targetMaterial == material) {
                return true;
            }
        }

        return false;
    }

    private boolean isInInfiniteCylinder(BeaconData beacon, Block block) {
        double dx = beacon.getLocation().getX() - (block.getX() + 0.5D);
        double dz = beacon.getLocation().getZ() - (block.getZ() + 0.5D);
        double range = beacon.getRange();

        return (dx * dx + dz * dz) <= range * range;
    }

    private static class FurnaceBoostData {
        private final double cookBoostPercent;
        private final double fuelBoostPercent;

        private FurnaceBoostData(double cookBoostPercent, double fuelBoostPercent) {
            this.cookBoostPercent = cookBoostPercent;
            this.fuelBoostPercent = fuelBoostPercent;
        }
    }
}