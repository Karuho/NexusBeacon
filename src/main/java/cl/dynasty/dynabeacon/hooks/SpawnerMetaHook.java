package cl.dynasty.dynabeacon.hooks;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;

import mc.rellox.spawnermeta.api.APIInstance;
import mc.rellox.spawnermeta.api.spawner.IGenerator;

public class SpawnerMetaHook {

    private boolean enabled;

    public void load() {
        enabled = Bukkit.getPluginManager().getPlugin("SpawnerMeta") != null;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean reduceTicks(Block block, int reduction, int minTicks) {
        if (!enabled || block == null) {
            return false;
        }

        try {
            IGenerator generator = APIInstance.api().getGenerator(block);

            if (generator == null || !generator.present() || !generator.valid()) {
                return false;
            }

            int current = generator.ticks();
            int next = Math.max(minTicks, current - reduction);

            Bukkit.getLogger().info("[DynaBeacon DEBUG] SpawnerMeta current ticks=" + current
                    + " reduction=" + reduction
                    + " next=" + next);

            generator.ticks(next);
            generator.update();

            Bukkit.getLogger().info("[DynaBeacon DEBUG] SpawnerMeta after ticks=" + generator.ticks());

            return true;
        } catch (Throwable throwable) {
            return false;
        }
    }
}