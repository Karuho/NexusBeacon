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

        if (section == null)
            return;

        int level = Math.max(1, beacon.getEffectLevel(effect.getId()));
        int radius = section.getInt("scan-radius", 16);
        int verticalRadius = section.getInt("vertical-radius", radius);
        int maxScannedBlocks = section.getInt("max-scanned-blocks-per-tick", 2000);
        int maxBlocks = section.getInt("max-blocks-per-tick", 16);

        // Delay base del spawner en ticks. Con SM instalado, SM controla este valor
        // internamente. Le pasamos el base para calcular el tope máximo del countdown.
        // Vanilla / SM default: 650 ticks (32.5 seg)
        int baseDelayTicks = section.getInt("base-delay-ticks", 650);

        // Reducción en ticks por nivel (60 ticks = 3 segundos por nivel)
        int reductionPerLevel = section.getInt("delay-reduction-ticks-per-level", 60);
        int totalReduction = reductionPerLevel * level;

        // Delay mínimo permitido en ticks (180 = 9 seg, así nivel 3 baja máximo 9 seg)
        int minDelayTicks = section.getInt("min-delay-ticks", 180);

        // El countdown máximo que permitimos: base - reducción, sin bajar del mínimo
        int targetMaxTicks = Math.max(minDelayTicks, baseDelayTicks - totalReduction);

        int scanned = 0;
        int processed = 0;

        Material spawnerMaterial = plugin.getVersionAdapter().material("SPAWNER");

        for (int x = -radius; x <= radius; x++) {
            for (int y = -verticalRadius; y <= verticalRadius; y++) {
                for (int z = -radius; z <= radius; z++) {

                    if (processed >= maxBlocks)
                        return;

                    if (++scanned >= maxScannedBlocks)
                        return;

                    Block block = center.getWorld().getBlockAt(
                            center.getBlockX() + x,
                            center.getBlockY() + y,
                            center.getBlockZ() + z);

                    if (block.getType() != spawnerMaterial)
                        continue;

                    // --- Integración SpawnerMeta ---
                    // Recortamos el countdown actual al máximo deseado.
                    // Si SM no está o falla, usamos el fallback vanilla.
                    if (plugin.getSpawnerMetaHook() != null && plugin.getSpawnerMetaHook().isEnabled()) {
                        boolean handled = plugin.getSpawnerMetaHook().setReducedDelay(block, targetMaxTicks);
                        if (handled) {
                            processed++;
                            continue;
                        }
                    }

                    // --- Fallback: Vanilla CreatureSpawner ---
                    if (!(block.getState() instanceof CreatureSpawner))
                        continue;

                    CreatureSpawner spawner = (CreatureSpawner) block.getState();

                    // Para vanilla seteamos min/max spawn delay del spawner directamente.
                    // Solo actualizamos si el delay actual es mayor al objetivo para no
                    // pisar configuraciones de otros plugins que ya lo bajaron más.
                    if (spawner.getMinSpawnDelay() > targetMaxTicks) {
                        spawner.setMinSpawnDelay(targetMaxTicks);
                        spawner.setMaxSpawnDelay(Math.max(targetMaxTicks, targetMaxTicks + 20));
                        spawner.update(true);

                        plugin.getLogger().fine("[DynaBeacon] Spawner vanilla actualizado: delay="
                                + targetMaxTicks + " ticks (nivel " + level + ") en "
                                + block.getWorld().getName()
                                + " " + block.getX() + "," + block.getY() + "," + block.getZ());
                    }

                    processed++;
                }
            }
        }
    }
}