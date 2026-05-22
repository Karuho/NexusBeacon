package cl.dynasty.dynabeacon.effects.executor;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.configuration.ConfigurationSection;

import cl.dynasty.dynabeacon.DynaBeaconPlugin;
import cl.dynasty.dynabeacon.effects.BeaconEffect;
import cl.dynasty.dynabeacon.model.BeaconData;

public class SpawnerBoostExecutor implements EffectExecutor {

    private final DynaBeaconPlugin plugin;

    public SpawnerBoostExecutor(DynaBeaconPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getType() {
        return "SPAWNER_BOOST";
    }

    @Override
    public void tick(BeaconData beacon, BeaconEffect effect) {
        Location center = beacon.getLocation();
        if (center == null || center.getWorld() == null)
            return;

        ConfigurationSection section = plugin.getConfigManager()
                .getEffectsConfig()
                .getConfigurationSection("effects." + effect.getId());

        int level = beacon.getEffectLevel(effect.getId());
        int radius = section != null ? section.getInt("scan-radius", 16) : 16;
        int verticalRadius = section != null ? section.getInt("vertical-radius", radius) : radius;

        int maxScannedBlocks = section.getInt("max-scanned-blocks-per-tick", 2000);
        int scanned = 0;

        int reduction = section != null ? section.getInt("delay-reduction-per-level", 20) * Math.max(1, level) : 20;
        int minDelay = section != null ? section.getInt("min-delay", 40) : 40;
        int maxBlocks = section != null ? section.getInt("max-blocks-per-tick", 16) : 16;

        int processed = 0;

        for (int x = -radius; x <= radius; x++) {
            for (int y = -verticalRadius; y <= verticalRadius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if (processed >= maxBlocks)
                        return;

                    scanned++;

                    if (scanned >= maxScannedBlocks) {
                        return;
                    }

                    Block block = center.getWorld().getBlockAt(
                            center.getBlockX() + x,
                            center.getBlockY() + y,
                            center.getBlockZ() + z);

                    Material spawnerMaterial = plugin.getVersionAdapter().material("SPAWNER");

                    plugin.getLogger().info("[DynaBeacon DEBUG] Spawner scan center="
                            + center.getWorld().getName()
                            + ";" + center.getBlockX()
                            + ";" + center.getBlockY()
                            + ";" + center.getBlockZ()
                            + " radius=" + radius
                            + " vertical=" + verticalRadius
                            + " material=" + spawnerMaterial);

                    if (block.getType() != spawnerMaterial) {
                        continue;
                    }

                    if (plugin.getSpawnerMetaHook() != null && plugin.getSpawnerMetaHook().isEnabled()) {
                        boolean handled = plugin.getSpawnerMetaHook().reduceTicks(block, reduction, minDelay);

                        if (handled) {
                            processed++;
                            continue;
                        }
                    }

                    if (!(block.getState() instanceof CreatureSpawner)) {
                        continue;
                    }

                    CreatureSpawner spawner = (CreatureSpawner) block.getState();

                    int newDelay = Math.max(minDelay, spawner.getDelay() - reduction);
                    spawner.setDelay(newDelay);
                    spawner.setMinSpawnDelay(Math.max(minDelay, spawner.getMinSpawnDelay() - reduction));
                    spawner.setMaxSpawnDelay(Math.max(minDelay + 20, spawner.getMaxSpawnDelay() - reduction));

                    spawner.update(true);
                    processed++;
                }
            }
        }
    }
}