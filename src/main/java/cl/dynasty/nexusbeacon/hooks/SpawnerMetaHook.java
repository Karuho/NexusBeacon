package cl.dynasty.nexusbeacon.hooks;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;

import mc.rellox.spawnermeta.api.APIInstance;
import mc.rellox.spawnermeta.api.spawner.ICache;
import mc.rellox.spawnermeta.api.spawner.IGenerator;
import mc.rellox.spawnermeta.api.spawner.ISpawner;

public class SpawnerMetaHook {

    private boolean enabled;

    public void load() {
        enabled = Bukkit.getPluginManager().getPlugin("SpawnerMeta") != null;
        if (enabled) {
            Bukkit.getLogger().info("[NexusBeacon] SpawnerMeta detectado y hook cargado.");
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Reduce el delay permanente del spawner en SpawnerMeta.
     *
     * ICache.delay() -> delay permanente actual que SM usa para este spawner.
     * ISpawner.setDelay(int) -> escribe el nuevo delay permanente.
     * IGenerator.ticks(int) -> recorta el countdown del ciclo actual para efecto inmediato.
     *
     * @param block          El bloque spawner
     * @param targetMaxTicks El delay máximo que queremos permitir (en ticks)
     * @return true si SM manejó el spawner, false para usar fallback vanilla
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

            // Log detallado para diagnóstico — nos dice exactamente qué ve SM
            Bukkit.getLogger().info("[NexusBeacon] SpawnerMeta: spawner en "
                    + block.getWorld().getName() + " " + block.getX() + "," + block.getY() + "," + block.getZ()
                    + " | cache.delay()=" + currentDelay
                    + " | generator.ticks()=" + generator.ticks()
                    + " | targetMaxTicks=" + targetMaxTicks);

            if (currentDelay > targetMaxTicks) {
                ISpawner spawner = generator.spawner();
                if (spawner == null) {
                    return false;
                }

                spawner.setDelay(targetMaxTicks);
                spawner.update();

                // También recortamos el countdown actual para efecto inmediato
                if (generator.ticks() > targetMaxTicks) {
                    generator.ticks(targetMaxTicks);
                    generator.update();
                }

                Bukkit.getLogger().info("[NexusBeacon] SpawnerMeta: delay actualizado "
                        + currentDelay + " -> " + targetMaxTicks + " ticks");
            } else {
                Bukkit.getLogger().info("[NexusBeacon] SpawnerMeta: delay " + currentDelay
                        + " ya es <= objetivo " + targetMaxTicks + ", sin cambios.");
            }

            return true;

        } catch (Throwable throwable) {
            Bukkit.getLogger().warning("[NexusBeacon] Error SpawnerMeta API: " + throwable.getClass().getSimpleName()
                    + " - " + throwable.getMessage());
            return false;
        }
    }
}