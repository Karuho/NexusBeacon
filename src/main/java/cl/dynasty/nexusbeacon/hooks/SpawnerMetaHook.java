package cl.dynasty.nexusbeacon.hooks;

import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;

import cl.dynasty.nexusbeacon.NexusBeaconPlugin;
import mc.rellox.spawnermeta.api.APIInstance;
import mc.rellox.spawnermeta.api.spawner.ICache;
import mc.rellox.spawnermeta.api.spawner.IGenerator;
import mc.rellox.spawnermeta.api.spawner.ISpawner;

public class SpawnerMetaHook {

    private boolean enabled;

    public void load() {
        enabled = Bukkit.getPluginManager().getPlugin("SpawnerMeta") != null;
        if (enabled) {
            Bukkit.getLogger().info("[NexusBeacon] SpawnerMeta detected and hook loaded.");
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * @param block
     * @param targetMaxTicks
     * @return
     */
    public boolean setReducedDelay(Block block, int targetMaxTicks) {
        if (!enabled || block == null) {
            return false;
        }

        try {
            IGenerator generator = APIInstance.api().getGenerator(block);

            if (generator == null || !generator.present() || !generator.valid()) {
                return false;
            }

            ICache cache = generator.cache();
            if (cache == null) {
                return false;
            }

            int currentDelay = cache.delay();

            if (currentDelay > targetMaxTicks) {
                ISpawner spawner = generator.spawner();
                if (spawner == null) {
                    return false;
                }

                spawner.setDelay(targetMaxTicks);
                spawner.update();

                if (generator.ticks() > targetMaxTicks) {
                    generator.ticks(targetMaxTicks);
                    generator.update();
                }

                Bukkit.getLogger().info(NexusBeaconPlugin.getInstance().getLanguageManager().get(
                        "console.spawnermeta-delay-updated",
                        Map.of(
                                "current", String.valueOf(currentDelay),
                                "target", String.valueOf(targetMaxTicks))));
            } else {
                Bukkit.getLogger().info(NexusBeaconPlugin.getInstance().getLanguageManager().get(
                        "console.spawnermeta-delay-unchanged",
                        Map.of(
                                "current", String.valueOf(currentDelay),
                                "target", String.valueOf(targetMaxTicks))));
            }

            return true;

        } catch (Throwable throwable) {
            Bukkit.getLogger().warning("[NexusBeacon] Error SpawnerMeta API: " + throwable.getClass().getSimpleName()
                    + " - " + throwable.getMessage());
            return false;
        }
    }
}