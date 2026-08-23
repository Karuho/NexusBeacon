package cl.dynasty.nexusbeacon.platform.legacy;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.Furnace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.Vector;

import cl.dynasty.nexusbeacon.platform.api.MaterialContext;
import cl.dynasty.nexusbeacon.platform.api.ScheduledTaskHandle;
import cl.dynasty.nexusbeacon.platform.api.SchedulerService;

/** Productive, state-authoritative Legacy effect loop. Every Bukkit mutation runs on the sync scheduler. */
public final class LegacyEffectRuntime implements Runnable {
    private final Plugin plugin;
    private final LegacyApplicationState state;
    private final LegacyEffectDefinitionRegistry definitions;
    private final LegacyEffectExecutorRegistry executors;
    private final SchedulerService scheduler;
    private final FileConfiguration effectsConfig;
    private final Material beaconMaterial;
    private final long intervalTicks;
    private ScheduledTaskHandle task;
    private volatile boolean running;

    public LegacyEffectRuntime(Plugin plugin, LegacyApplicationState state, FileConfiguration beaconConfig,
            FileConfiguration effectsConfig, LegacyMaterialResolver materials,
            LegacyPotionEffectResolver potions, SchedulerService scheduler) {
        if (plugin == null || state == null || beaconConfig == null || effectsConfig == null
                || materials == null || potions == null || scheduler == null) throw new NullPointerException();
        if (!state.getStatus().isReady()) throw new IllegalStateException("Effect runtime requires ready state");
        this.plugin = plugin;
        this.state = state;
        this.scheduler = scheduler;
        this.effectsConfig = effectsConfig;
        this.definitions = new LegacyEffectDefinitionRegistry(effectsConfig, materials, potions);
        this.executors = new LegacyEffectExecutorRegistry(plugin, beaconConfig, effectsConfig, materials);
        LegacyMaterialResolution beacon = materials.resolveLegacyMaterial("BEACON", MaterialContext.BLOCK_MATCH);
        if (!beacon.getResolution().isResolved()) throw new IllegalStateException("BEACON block is unavailable");
        this.beaconMaterial = beacon.getResolution().getMaterial().get();
        this.intervalTicks = Math.max(1L, beaconConfig.getLong("beacon.tick-interval", 40L));
    }

    public synchronized void start() {
        if (running) throw new IllegalStateException("Legacy effect runtime is already running");
        if (!state.getStatus().isReady()) throw new IllegalStateException("Effect runtime requires ready state");
        running = true;
        task = scheduler.runSyncTimer(this, intervalTicks, intervalTicks);
    }

    public LegacyEffectDefinition getDefinition(String id) { return definitions.get(id); }

    /** Reuses the authoritative definition registry for Modern-equivalent furnace event selection. */
    public LegacyFurnaceBoost findBestFurnaceBoost(Block block) {
        if (block == null || !running || !state.getStatus().isReady()) return null;
        LegacyFurnaceBoost best = null;
        for (LegacyBeaconState beacon : state.snapshot()) {
            LegacyBeaconLocation location = beacon.getLocation();
            if (!location.getWorldName().equals(block.getWorld().getName())) continue;
            double dx = location.getX() - (block.getX() + 0.5D);
            double dz = location.getZ() - (block.getZ() + 0.5D);
            if (dx * dx + dz * dz > beacon.getRange() * beacon.getRange()) continue;
            for (String effectId : beacon.getActiveEffects()) {
                LegacyEffectDefinition definition = definitions.get(effectId);
                Integer acquired = beacon.getEffectLevels().get(effectId.toLowerCase(Locale.ROOT));
                if (definition == null || acquired == null || !definition.isSupported()
                        || !"BLOCK_PROCESS_BOOST".equals(definition.getType())
                        || !definition.getTargetBlocks().contains(block.getType())) continue;
                int level = Math.min(definition.getMaxLevel(), Math.max(1, acquired.intValue()));
                double cook = levelDouble(effectsConfig, definition.getId(), level, "speed-up-time",
                        effectsConfig.getDouble("effects." + definition.getId()
                                + ".speed-up-time-per-level", 15.0D) * level);
                double fuel = levelDouble(effectsConfig, definition.getId(), level, "fuel-speed-up-time",
                        effectsConfig.getDouble("effects." + definition.getId()
                                + ".fuel-speed-up-time-per-level", cook) * level);
                LegacyFurnaceBoost candidate = new LegacyFurnaceBoost(cook, fuel);
                if (best == null || candidate.getCookPercent() > best.getCookPercent()) best = candidate;
            }
        }
        return best;
    }

    @Override
    public void run() {
        if (!running || !state.getStatus().isReady()) return;
        if (!Bukkit.isPrimaryThread()) {
            plugin.getLogger().severe("Legacy effect tick rejected off the primary thread.");
            return;
        }
        Collection<LegacyBeaconState> snapshot = state.snapshot();
        Set<String> activeBeaconIds = new HashSet<String>();
        for (LegacyBeaconState beacon : snapshot) {
            activeBeaconIds.add(beacon.getId());
            LegacyBeaconLocation stored = beacon.getLocation();
            World world = plugin.getServer().getWorld(stored.getWorldName());
            if (world == null || !world.isChunkLoaded(stored.getX() >> 4, stored.getZ() >> 4)) continue;
            Block physical = world.getBlockAt(stored.getX(), stored.getY(), stored.getZ());
            if (physical.getType() != beaconMaterial) continue;
            Location center = new Location(world, stored.getX(), stored.getY(), stored.getZ());
            for (String effectId : beacon.getActiveEffects()) {
                LegacyEffectDefinition definition = definitions.get(effectId);
                Integer acquiredLevel = beacon.getEffectLevels().get(effectId.toLowerCase(Locale.ROOT));
                if (definition == null || !definition.isSupported() || acquiredLevel == null) continue;
                int level = Math.min(definition.getMaxLevel(), Math.max(1, acquiredLevel.intValue()));
                try {
                    executors.execute(beacon, center, definition, level);
                } catch (RuntimeException failure) {
                    plugin.getLogger().warning("Legacy effect " + definition.getId() + " failed safely at "
                            + beacon.getId() + ": " + failure.getClass().getSimpleName() + ": "
                            + failure.getMessage());
                }
            }
        }
        executors.retainBeacons(activeBeaconIds);
    }

    public synchronized void close() {
        running = false;
        if (task != null) task.cancel();
        task = null;
        executors.clear();
    }

    public boolean isRunning() { return running; }
    public long getIntervalTicks() { return intervalTicks; }
    public int getDefinitionCount() { return definitions.size(); }
    public int getSupportedDefinitionCount() { return definitions.supportedCount(); }
    public int getExecutorCount() { return executors.size(); }
    public LegacyEffectDefinitionRegistry getDefinitions() { return definitions; }

    private interface LegacyEffectExecutor {
        String type();
        void execute(LegacyBeaconState beacon, Location center, LegacyEffectDefinition definition, int level);
        void retainBeacons(Set<String> activeBeaconIds);
        void clear();
    }

    private abstract static class StatelessExecutor implements LegacyEffectExecutor {
        @Override public void retainBeacons(Set<String> activeBeaconIds) { }
        @Override public void clear() { }
    }

    private static final class LegacyEffectExecutorRegistry {
        private final Map<String, LegacyEffectExecutor> byType =
                new LinkedHashMap<String, LegacyEffectExecutor>();

        private LegacyEffectExecutorRegistry(Plugin plugin, FileConfiguration beacon,
                FileConfiguration effects, LegacyMaterialResolver materials) {
            register(new PotionExecutor());
            register(new CropExecutor(beacon, effects, materials));
            register(new SpawnerExecutor(plugin, beacon, effects, materials));
            register(new IgnitionExecutor(effects));
            register(new DamageExecutor(effects));
            register(new GravityExecutor(effects));
            register(new BlockProcessExecutor(effects));
            if (byType.size() != 7) throw new IllegalStateException("Expected seven Legacy effect executors");
        }

        private void register(LegacyEffectExecutor executor) {
            if (byType.put(executor.type(), executor) != null) {
                throw new IllegalStateException("Duplicate Legacy effect executor: " + executor.type());
            }
        }
        private void execute(LegacyBeaconState beacon, Location center,
                LegacyEffectDefinition definition, int level) {
            LegacyEffectExecutor executor = byType.get(definition.getType());
            if (executor != null) executor.execute(beacon, center, definition, level);
        }
        private void retainBeacons(Set<String> ids) {
            for (LegacyEffectExecutor executor : byType.values()) executor.retainBeacons(ids);
        }
        private void clear() {
            for (LegacyEffectExecutor executor : byType.values()) executor.clear();
        }
        private int size() { return byType.size(); }
    }

    private static final class PotionExecutor extends StatelessExecutor {
        @Override public String type() { return "POTION"; }
        @Override public void execute(LegacyBeaconState beacon, Location center,
                LegacyEffectDefinition definition, int level) {
            if (definition.getPotion() == null) return;
            PotionEffect potion = new PotionEffect(definition.getPotion(), definition.getPotionDurationTicks(),
                    Math.max(0, level * definition.getAmplifierPerLevel() - 1), true, true);
            for (Entity entity : nearby(center, beacon.getRange())) {
                if (!(entity instanceof LivingEntity) || !insideHorizontal(entity.getLocation(), center, beacon.getRange())) continue;
                LivingEntity living = (LivingEntity) entity;
                if (matchesTarget(living, definition.getTarget())) living.addPotionEffect(potion, true);
            }
        }
    }

    private static final class CropExecutor implements LegacyEffectExecutor {
        private final FileConfiguration beaconConfig;
        private final FileConfiguration effectsConfig;
        private final LegacyCropBackend crops;
        private final Map<String, Integer> cursors = new HashMap<String, Integer>();
        private final Random random = new Random();
        private CropExecutor(FileConfiguration beaconConfig, FileConfiguration effectsConfig,
                LegacyMaterialResolver materials) {
            this.beaconConfig = beaconConfig;
            this.effectsConfig = effectsConfig;
            this.crops = new LegacyCropBackend(materials);
        }
        @Override public String type() { return "CROP_BOOST"; }
        @Override public void execute(LegacyBeaconState beacon, Location center,
                LegacyEffectDefinition definition, int level) {
            int radius = Math.max(0, Math.min(beacon.getRange(),
                    beaconConfig.getInt("performance.crop-boost.scan-radius", beacon.getRange())));
            int vertical = Math.max(0, beaconConfig.getInt("performance.crop-boost.vertical-radius", 8));
            int maxBlocks = Math.max(0, beaconConfig.getInt("performance.crop-boost.max-blocks-per-tick", 16));
            int maxScanned = Math.max(0,
                    beaconConfig.getInt("performance.crop-boost.max-scanned-blocks-per-tick", 2000));
            int chance = levelInt(effectsConfig, definition.getId(), level, "growth-chance", 15 * level);
            int width = radius * 2 + 1;
            int total = width * width;
            String key = beacon.getId() + ":" + definition.getId();
            int cursor = value(cursors.get(key));
            int processed = 0;
            int scanned = 0;
            while (scanned < maxScanned && processed < maxBlocks && total > 0) {
                int index = cursor % total;
                int x = index / width - radius;
                int z = index % width - radius;
                cursor++;
                scanned++;
                if (x * x + z * z > radius * radius) continue;
                int blockX = center.getBlockX() + x;
                int blockZ = center.getBlockZ() + z;
                if (!center.getWorld().isChunkLoaded(blockX >> 4, blockZ >> 4)) continue;
                for (int y = -vertical; y <= vertical; y++) {
                    int blockY = center.getBlockY() + y;
                    if (blockY < 0 || blockY >= center.getWorld().getMaxHeight()) continue;
                    Block block = center.getWorld().getBlockAt(blockX, blockY, blockZ);
                    if (!crops.isImmature(block)) continue;
                    if (random.nextDouble() * 100.0D > chance) continue;
                    if (crops.growOneStage(block) == LegacyCropBackend.GrowthResult.GROWN) {
                        processed++;
                        break;
                    }
                }
            }
            cursors.put(key, Integer.valueOf(cursor % total));
        }
        @Override public void retainBeacons(Set<String> activeBeaconIds) {
            Iterator<String> iterator = cursors.keySet().iterator();
            while (iterator.hasNext()) {
                String key = iterator.next();
                int separator = key.lastIndexOf(':');
                if (separator < 0 || !activeBeaconIds.contains(key.substring(0, separator))) iterator.remove();
            }
        }
        @Override public void clear() { cursors.clear(); }
    }

    private static final class IgnitionExecutor extends EntityEffectExecutor {
        private IgnitionExecutor(FileConfiguration effects) { super(effects); }
        @Override public String type() { return "IGNITION"; }
        @Override protected void mutate(LivingEntity entity, Location center,
                LegacyEffectDefinition definition, int level) {
            entity.setFireTicks(levelInt(effects, definition.getId(), level, "fire-ticks", 60 * level));
        }
    }

    private static final class DamageExecutor extends EntityEffectExecutor {
        private DamageExecutor(FileConfiguration effects) { super(effects); }
        @Override public String type() { return "DAMAGE_FIELD"; }
        @Override protected void mutate(LivingEntity entity, Location center,
                LegacyEffectDefinition definition, int level) {
            entity.damage(levelDouble(effects, definition.getId(), level, "damage", 1.0D * level));
        }
    }

    private static final class GravityExecutor extends EntityEffectExecutor {
        private GravityExecutor(FileConfiguration effects) { super(effects); }
        @Override public String type() { return "GRAVITY_PULSE"; }
        @Override protected void mutate(LivingEntity entity, Location center,
                LegacyEffectDefinition definition, int level) {
            double strength = levelDouble(effects, definition.getId(), level, "pull-strength", 0.08D * level);
            double vertical = levelDouble(effects, definition.getId(), level, "vertical-boost", 0.05D);
            double maximum = levelDouble(effects, definition.getId(), level, "max-velocity", 1.2D);
            Location target = center.clone().add(0.5D, 1.0D, 0.5D);
            Vector direction = target.toVector().subtract(entity.getLocation().toVector());
            if (direction.lengthSquared() <= 0.01D) return;
            direction.normalize().multiply(strength).setY(vertical);
            Vector velocity = entity.getVelocity().add(direction);
            if (velocity.length() > maximum) velocity.normalize().multiply(maximum);
            entity.setVelocity(velocity);
        }
    }

    private abstract static class EntityEffectExecutor extends StatelessExecutor {
        protected final FileConfiguration effects;
        private EntityEffectExecutor(FileConfiguration effects) { this.effects = effects; }
        @Override public final void execute(LegacyBeaconState beacon, Location center,
                LegacyEffectDefinition definition, int level) {
            for (Entity entity : nearby(center, beacon.getRange())) {
                if (entity instanceof LivingEntity && isHostile(entity)
                        && insideHorizontal(entity.getLocation(), center, beacon.getRange())) {
                    mutate((LivingEntity) entity, center, definition, level);
                }
            }
        }
        protected abstract void mutate(LivingEntity entity, Location center,
                LegacyEffectDefinition definition, int level);
    }

    private static final class BlockProcessExecutor extends StatelessExecutor {
        private final FileConfiguration effects;
        private BlockProcessExecutor(FileConfiguration effects) { this.effects = effects; }
        @Override public String type() { return "BLOCK_PROCESS_BOOST"; }
        @Override public void execute(LegacyBeaconState beacon, Location center,
                LegacyEffectDefinition definition, int level) {
            ConfigurationSection section = effects.getConfigurationSection("effects." + definition.getId());
            if (section == null) return;
            int radius = Math.max(0, Math.min(beacon.getRange(), section.getInt("scan-radius", beacon.getRange())));
            int vertical = Math.max(0, section.getInt("vertical-radius", radius));
            int maxProcessed = Math.max(0, section.getInt("max-blocks-per-tick", 32));
            int maxScanned = Math.max(0, section.getInt("max-scanned-blocks-per-tick", 2000));
            int processed = 0;
            int scanned = 0;
            for (int x = -radius; x <= radius && processed < maxProcessed && scanned < maxScanned; x++) {
                for (int y = -vertical; y <= vertical && processed < maxProcessed && scanned < maxScanned; y++) {
                    for (int z = -radius; z <= radius && processed < maxProcessed && scanned < maxScanned; z++) {
                        scanned++;
                        int bx = center.getBlockX() + x;
                        int by = center.getBlockY() + y;
                        int bz = center.getBlockZ() + z;
                        if (by < 0 || by >= center.getWorld().getMaxHeight()
                                || !center.getWorld().isChunkLoaded(bx >> 4, bz >> 4)) continue;
                        Block block = center.getWorld().getBlockAt(bx, by, bz);
                        if (!matchesFurnaceTarget(block.getType(), definition.getTargetBlocks())) continue;
                        if (!(block.getState() instanceof Furnace)) continue;
                        Furnace furnace = (Furnace) block.getState();
                        if (furnace.getBurnTime() <= 0) continue;
                        int speed = levelInt(effects, definition.getId(), level, "speed-up-time", 8);
                        int fuel = levelInt(effects, definition.getId(), level, "fuel-speed-up-time", speed);
                        int total = reflectedInt(furnace, "getCookTimeTotal", 200);
                        short current = furnace.getCookTime();
                        furnace.setCookTime((short) Math.min(total - 1, current + speed));
                        if (furnace.getBurnTime() < fuel) furnace.setBurnTime((short) fuel);
                        furnace.update(true);
                        processed++;
                    }
                }
            }
        }
    }

    private static final class SpawnerExecutor implements LegacyEffectExecutor {
        private final Plugin plugin;
        private final FileConfiguration beaconConfig;
        private final FileConfiguration effects;
        private final Material spawnerMaterial;
        private final Map<String, Long> cooldowns = new HashMap<String, Long>();
        private final Random random = new Random();
        private SpawnerExecutor(Plugin plugin, FileConfiguration beaconConfig, FileConfiguration effects,
                LegacyMaterialResolver materials) {
            this.plugin = plugin;
            this.beaconConfig = beaconConfig;
            this.effects = effects;
            LegacyMaterialResolution spawner = materials.resolveLegacyMaterial("SPAWNER", MaterialContext.BLOCK_MATCH);
            this.spawnerMaterial = spawner.getResolution().isResolved()
                    ? spawner.getResolution().getMaterial().get() : null;
        }
        @Override public String type() { return "SPAWNER_BOOST"; }
        @Override public void execute(LegacyBeaconState beacon, Location center,
                LegacyEffectDefinition definition, int level) {
            if (spawnerMaterial == null || (beaconConfig.getBoolean(
                    "performance.spawner-boost.disable-when-spawnermeta-enabled", true)
                    && plugin.getServer().getPluginManager().isPluginEnabled("SpawnerMeta"))) return;
            int radius = Math.max(0, beaconConfig.getInt("performance.spawner-boost.scan-radius", 16));
            int vertical = Math.max(0, beaconConfig.getInt("performance.spawner-boost.vertical-radius", radius));
            int maxProcessed = Math.max(0,
                    beaconConfig.getInt("performance.spawner-boost.max-blocks-per-tick", 8));
            int maxScanned = Math.max(0,
                    beaconConfig.getInt("performance.spawner-boost.max-scanned-blocks-per-tick", 2000));
            double percent = levelDouble(effects, definition.getId(), level,
                    "speed-up-percentage", 15.0D * level);
            double factor = Math.min(0.95D, percent / 100.0D);
            int cooldownTicks = levelInt(effects, definition.getId(), level, "cooldown-ticks", 200);
            long now = System.currentTimeMillis();
            int processed = 0;
            int scanned = 0;
            for (int x = -radius; x <= radius && processed < maxProcessed && scanned < maxScanned; x++) {
                for (int y = -vertical; y <= vertical && processed < maxProcessed && scanned < maxScanned; y++) {
                    for (int z = -radius; z <= radius && processed < maxProcessed && scanned < maxScanned; z++) {
                        scanned++;
                        int bx = center.getBlockX() + x;
                        int by = center.getBlockY() + y;
                        int bz = center.getBlockZ() + z;
                        if (by < 0 || by >= center.getWorld().getMaxHeight()
                                || !center.getWorld().isChunkLoaded(bx >> 4, bz >> 4)) continue;
                        Block block = center.getWorld().getBlockAt(bx, by, bz);
                        if (block.getType() != spawnerMaterial || !(block.getState() instanceof CreatureSpawner)) continue;
                        String blockKey = block.getWorld().getName() + ";" + block.getX() + ";"
                                + block.getY() + ";" + block.getZ();
                        Long until = cooldowns.get(blockKey);
                        if (until != null && now < until.longValue()) continue;
                        CreatureSpawner spawner = (CreatureSpawner) block.getState();
                        int minimum = Math.max(1, reflectedInt(spawner, "getMinSpawnDelay", 200));
                        int maximum = Math.max(minimum + 1, reflectedInt(spawner, "getMaxSpawnDelay", 800));
                        int base = random.nextInt(maximum - minimum) + minimum;
                        spawner.setDelay(Math.max(1, (int) Math.round(base * (1.0D - factor))));
                        spawner.update(true);
                        cooldowns.put(blockKey, Long.valueOf(now + cooldownTicks * 50L));
                        processed++;
                    }
                }
            }
            cleanup(now);
        }
        private void cleanup(long now) {
            Iterator<Map.Entry<String, Long>> iterator = cooldowns.entrySet().iterator();
            while (iterator.hasNext()) if (iterator.next().getValue().longValue() < now) iterator.remove();
        }
        @Override public void retainBeacons(Set<String> activeBeaconIds) { cleanup(System.currentTimeMillis()); }
        @Override public void clear() { cooldowns.clear(); }
    }

    private static Collection<Entity> nearby(Location center, int range) {
        return center.getWorld().getNearbyEntities(center, range, range, range);
    }

    private static boolean insideHorizontal(Location location, Location center, int range) {
        if (location == null || location.getWorld() == null || !location.getWorld().equals(center.getWorld())) return false;
        double dx = location.getX() - center.getX();
        double dz = location.getZ() - center.getZ();
        return dx * dx + dz * dz <= range * range;
    }

    private static boolean matchesTarget(LivingEntity entity, String target) {
        if ("ALL_ENTITIES".equalsIgnoreCase(target)) return true;
        if ("MONSTERS".equalsIgnoreCase(target)) return entity instanceof Monster;
        if ("ANIMALS".equalsIgnoreCase(target)) return entity instanceof Animals;
        return entity instanceof Player;
    }

    private static boolean isHostile(Entity entity) {
        if (entity instanceof Monster) return true;
        String type = entity.getType().name();
        return "PHANTOM".equals(type) || "SLIME".equals(type) || "MAGMA_CUBE".equals(type)
                || "GHAST".equals(type) || "SHULKER".equals(type);
    }

    private static int levelInt(FileConfiguration config, String id, int level, String key, int fallback) {
        String levelPath = "effects." + id + ".levels." + level + "." + key;
        return config.contains(levelPath) ? config.getInt(levelPath, fallback)
                : config.getInt("effects." + id + "." + key, fallback);
    }

    private static double levelDouble(FileConfiguration config, String id, int level, String key, double fallback) {
        String levelPath = "effects." + id + ".levels." + level + "." + key;
        return config.contains(levelPath) ? config.getDouble(levelPath, fallback)
                : config.getDouble("effects." + id + "." + key, fallback);
    }

    private static int reflectedInt(Object target, String methodName, int fallback) {
        try {
            Method method = target.getClass().getMethod(methodName);
            Object value = method.invoke(target);
            return value instanceof Number ? ((Number) value).intValue() : fallback;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static boolean matchesFurnaceTarget(Material actual, List<Material> targets) {
        if (targets.contains(actual)) return true;
        return "BURNING_FURNACE".equals(actual.name()) && targets.contains(Material.getMaterial("FURNACE"));
    }

    private static int value(Integer value) { return value == null ? 0 : value.intValue(); }
}
