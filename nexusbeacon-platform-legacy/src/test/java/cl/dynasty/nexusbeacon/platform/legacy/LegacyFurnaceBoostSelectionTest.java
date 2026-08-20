package cl.dynasty.nexusbeacon.platform.legacy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import cl.dynasty.nexusbeacon.platform.api.ScheduledTaskHandle;
import cl.dynasty.nexusbeacon.platform.api.SchedulerService;

class LegacyFurnaceBoostSelectionTest {
    @Test void activeEligibleEffectReturnsConfiguredLevelAndBurnListenerUsesIt() {
        Fixture fixture = new Fixture();
        fixture.add("world", 0, 64, 0, 48, 2, true);
        fixture.start();

        LegacyFurnaceBoost boost = fixture.runtime.findBestFurnaceBoost(block("world", 4, 70, 0, Material.FURNACE));

        assertEquals(16.0D, boost.getCookPercent());
        assertEquals(12.0D, boost.getFuelPercent());
        org.bukkit.event.inventory.FurnaceBurnEvent event = new org.bukkit.event.inventory.FurnaceBurnEvent(
                block("world", 4, 70, 0, Material.FURNACE), null, 1000);
        new LegacyFurnaceBoostListener(fixture.runtime, fixture.scheduler).onFurnaceBurn(event);
        assertEquals(880, event.getBurnTime());
    }

    @Test void inactiveWrongWorldWrongTargetAndRemovedBeaconDoNotContribute() {
        Fixture fixture = new Fixture();
        LegacyBeaconState inactive = fixture.add("world", 0, 64, 0, 48, 1, false);
        fixture.start();
        assertNull(fixture.runtime.findBestFurnaceBoost(block("world", 1, 64, 1, Material.FURNACE)));
        assertNull(fixture.runtime.findBestFurnaceBoost(block("other", 1, 64, 1, Material.FURNACE)));
        assertNull(fixture.runtime.findBestFurnaceBoost(block("world", 1, 64, 1, Material.CHEST)));
        fixture.state.delete(inactive.getLocation());
        assertNull(fixture.runtime.findBestFurnaceBoost(block("world", 1, 64, 1, Material.FURNACE)));
    }

    @Test void overlappingBeaconsChooseOneHighestCookBoostWithoutStacking() {
        Fixture fixture = new Fixture();
        fixture.add("world", 0, 64, 0, 48, 1, true);
        fixture.add("world", 8, 64, 0, 48, 3, true);
        fixture.start();

        LegacyFurnaceBoost boost = fixture.runtime.findBestFurnaceBoost(block("world", 4, 20, 0, Material.FURNACE));

        assertEquals(24.0D, boost.getCookPercent());
        assertEquals(18.0D, boost.getFuelPercent());
    }

    private static final class Fixture {
        private final Storage storage = new Storage();
        private final LegacyApplicationState state = new LegacyApplicationState(storage);
        private final Scheduler scheduler = new Scheduler();
        private final LegacyEffectRuntime runtime;
        private Fixture() {
            state.initialize();
            FileConfiguration beacon = new YamlConfiguration();
            beacon.set("beacon.tick-interval", 40);
            FileConfiguration effects = new YamlConfiguration();
            effects.set("effects.furnace_boost.enabled", true);
            effects.set("effects.furnace_boost.type", "BLOCK_PROCESS_BOOST");
            effects.set("effects.furnace_boost.target-blocks", Collections.singletonList("FURNACE"));
            effects.set("effects.furnace_boost.max-level", 3);
            effects.set("effects.furnace_boost.levels.1.speed-up-time", 8);
            effects.set("effects.furnace_boost.levels.1.fuel-speed-up-time", 6);
            effects.set("effects.furnace_boost.levels.2.speed-up-time", 16);
            effects.set("effects.furnace_boost.levels.2.fuel-speed-up-time", 12);
            effects.set("effects.furnace_boost.levels.3.speed-up-time", 24);
            effects.set("effects.furnace_boost.levels.3.fuel-speed-up-time", 18);
            Plugin plugin = (Plugin) Proxy.newProxyInstance(Plugin.class.getClassLoader(),
                    new Class<?>[] { Plugin.class }, (proxy, method, args) -> null);
            runtime = new LegacyEffectRuntime(plugin, state, beacon, effects, new LegacyMaterialResolver(),
                    new LegacyPotionEffectResolver(), scheduler);
        }
        private LegacyBeaconState add(String world, int x, int y, int z, int range, int level, boolean active) {
            Map<String, Integer> levels = new LinkedHashMap<String, Integer>();
            levels.put("furnace_boost", Integer.valueOf(level));
            Set<String> activeEffects = new LinkedHashSet<String>();
            if (active) activeEffects.add("furnace_boost");
            LegacyBeaconState beacon = new LegacyBeaconState(new LegacyBeaconLocation(world, x, y, z),
                    UUID.randomUUID(), UUID.randomUUID(), range, 1, levels, activeEffects,
                    Collections.<UUID>emptySet(), true, "aqua", true, "VILLAGER_HAPPY");
            state.insert(beacon);
            return beacon;
        }
        private void start() { runtime.start(); }
    }

    private static Block block(final String worldName, final int x, final int y, final int z, final Material type) {
        final World world = (World) Proxy.newProxyInstance(World.class.getClassLoader(), new Class<?>[] { World.class },
                (proxy, method, args) -> "getName".equals(method.getName()) ? worldName : primitive(method.getReturnType()));
        return (Block) Proxy.newProxyInstance(Block.class.getClassLoader(), new Class<?>[] { Block.class },
                (proxy, method, args) -> "getWorld".equals(method.getName()) ? world
                        : "getX".equals(method.getName()) ? Integer.valueOf(x)
                        : "getY".equals(method.getName()) ? Integer.valueOf(y)
                        : "getZ".equals(method.getName()) ? Integer.valueOf(z)
                        : "getType".equals(method.getName()) ? type : primitive(method.getReturnType()));
    }

    private static Object primitive(Class<?> type) {
        if (type == Boolean.TYPE) return Boolean.FALSE;
        if (type == Integer.TYPE) return Integer.valueOf(0);
        if (type == Short.TYPE) return Short.valueOf((short) 0);
        if (type == Byte.TYPE) return Byte.valueOf((byte) 0);
        if (type == Long.TYPE) return Long.valueOf(0L);
        if (type == Float.TYPE) return Float.valueOf(0F);
        if (type == Double.TYPE) return Double.valueOf(0D);
        return null;
    }

    private static final class Storage implements LegacyBeaconStorage {
        private final List<LegacyBeaconState> values = new ArrayList<LegacyBeaconState>();
        @Override public LegacyStorageLoadResult load() { return LegacyStorageLoadResult.success(values); }
        @Override public void store(Collection<LegacyBeaconState> beacons) { values.clear(); values.addAll(beacons); }
        @Override public void close() { }
        @Override public String getBackendName() { return "TEST"; }
    }

    private static final class Scheduler implements SchedulerService {
        @Override public void runSync(Runnable runnable) { runnable.run(); }
        @Override public void runSync(Location location, Runnable runnable) { runnable.run(); }
        @Override public void runSync(Entity entity, Runnable runnable) { runnable.run(); }
        @Override public ScheduledTaskHandle runSyncLater(Runnable runnable, long delayTicks) { return handle(); }
        @Override public ScheduledTaskHandle runSyncTimer(Runnable runnable, long delayTicks, long intervalTicks) {
            return handle();
        }
        @Override public void runAsync(Runnable runnable) { }
        @Override public ScheduledTaskHandle runAsyncLater(Runnable runnable, long delayTicks) { return handle(); }
        @Override public ScheduledTaskHandle runAsyncTimer(Runnable runnable, long delayTicks, long intervalTicks) {
            return handle();
        }
        private ScheduledTaskHandle handle() { return new ScheduledTaskHandle() { @Override public void cancel() { } }; }
    }
}
