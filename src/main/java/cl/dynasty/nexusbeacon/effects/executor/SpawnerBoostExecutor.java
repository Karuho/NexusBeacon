package cl.dynasty.nexusbeacon.effects.executor;

import cl.dynasty.nexusbeacon.NexusBeaconPlugin;
import cl.dynasty.nexusbeacon.effects.BeaconEffect;
import cl.dynasty.nexusbeacon.effects.EffectLevelUtil;
import cl.dynasty.nexusbeacon.model.BeaconData;
import cl.dynasty.nexusbeacon.util.DebugLogger;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class SpawnerBoostExecutor implements EffectExecutor {

    private final NexusBeaconPlugin plugin;
    private final Random random = new Random();
    private final Map<Location, Long> cooldowns = new HashMap<Location, Long>();

    public SpawnerBoostExecutor(NexusBeaconPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getType() {
        return "SPAWNER_BOOST";
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

        int radius = plugin.getConfigManager().getBeaconConfig()
                .getInt("performance.spawner-boost.scan-radius", 16);

        int verticalRadius = plugin.getConfigManager().getBeaconConfig()
                .getInt("performance.spawner-boost.vertical-radius", radius);

        int maxProcessed = plugin.getConfigManager().getBeaconConfig()
                .getInt("performance.spawner-boost.max-blocks-per-tick", 8);

        int maxScanned = plugin.getConfigManager().getBeaconConfig()
                .getInt("performance.spawner-boost.max-scanned-blocks-per-tick", 2000);

        double speedUpPercentage = EffectLevelUtil.getLevelDouble(
                plugin,
                effect,
                level,
                "speed-up-percentage",
                section.getDouble("speed-up-percentage-per-level", 15.0D) * level);

        double speedUpFactor = Math.min(0.95D, speedUpPercentage / 100.0D);

        int cooldownTicks = EffectLevelUtil.getLevelInt(
                plugin,
                effect,
                level,
                "cooldown-ticks",
                section.getInt("cooldown-ticks", 200));
        DebugLogger.log(plugin, effect.getType() + ":" + beacon.getId() + ":scan",
                "EffectExecutor type=" + effect.getType()
                        + " effect=" + effect.getId()
                        + " level=" + level
                        + " radius=" + radius
                        + " verticalRadius=" + verticalRadius
                        + " maxProcessed=" + maxProcessed
                        + " speedUpPercentage=" + speedUpPercentage
                        + " speedUpFactor=" + speedUpFactor
                        + " cooldownTicks=" + cooldownTicks
                        + " range=" + beacon.getRange());
        long now = System.currentTimeMillis();

        Material spawnerMaterial = plugin.getVersionAdapter().material("SPAWNER");

        int processed = 0;
        int scanned = 0;

        for (int x = -radius; x <= radius; x++) {
            for (int y = -verticalRadius; y <= verticalRadius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if (processed >= maxProcessed) {
                        cleanupCooldowns(now);
                        return;
                    }

                    if (++scanned >= maxScanned) {
                        cleanupCooldowns(now);
                        return;
                    }

                    Block block = center.getWorld().getBlockAt(
                            center.getBlockX() + x,
                            center.getBlockY() + y,
                            center.getBlockZ() + z);

                    if (block.getType() != spawnerMaterial) {
                        continue;
                    }

                    if (!(block.getState() instanceof CreatureSpawner)) {
                        continue;
                    }

                    CreatureSpawner spawner = (CreatureSpawner) block.getState();

                    boolean disableWithSpawnerMeta = plugin.getConfigManager().getBeaconConfig()
                            .getBoolean("performance.spawner-boost.disable-when-spawnermeta-enabled", true);

                    if (disableWithSpawnerMeta
                            && plugin.getServer().getPluginManager().isPluginEnabled("SpawnerMeta")) {
                        continue;
                    }

                    Long cooldownUntil = cooldowns.get(block.getLocation());
                    if (cooldownUntil != null && now < cooldownUntil) {
                        continue;
                    }

                    int minDelay = Math.max(1, spawner.getMinSpawnDelay());
                    int maxDelay = Math.max(minDelay + 1, spawner.getMaxSpawnDelay());

                    int baseDelay = random.nextInt(maxDelay - minDelay) + minDelay;
                    int boostedDelay = Math.max(1, (int) Math.round(baseDelay * (1.0D - speedUpFactor)));

                    spawner.setDelay(boostedDelay);
                    spawner.update(true);

                    DebugLogger.log(plugin, effect.getType() + ":" + beacon.getId() + ":process",
                            "SpawnerBoost applied effect=" + effect.getId()
                                    + " level=" + level
                                    + " location=" + block.getWorld().getName()
                                    + "," + block.getX()
                                    + "," + block.getY()
                                    + "," + block.getZ()
                                    + " baseDelay=" + baseDelay
                                    + " boostedDelay=" + boostedDelay
                                    + " cooldownTicks=" + cooldownTicks);

                    cooldowns.put(block.getLocation(), now + (cooldownTicks * 50L));
                    processed++;
                }
            }
        }

        cleanupCooldowns(now);
    }

    private void cleanupCooldowns(long now) {
        cooldowns.entrySet().removeIf(entry -> entry.getValue() < now);
    }
}