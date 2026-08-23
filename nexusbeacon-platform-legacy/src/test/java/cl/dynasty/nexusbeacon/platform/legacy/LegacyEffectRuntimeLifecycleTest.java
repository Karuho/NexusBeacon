package cl.dynasty.nexusbeacon.platform.legacy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.Collections;

import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;

import cl.dynasty.nexusbeacon.platform.api.ScheduledTaskHandle;
import cl.dynasty.nexusbeacon.platform.api.SchedulerService;

class LegacyEffectRuntimeLifecycleTest {
    @Test void schedulesOneSyncLoopAndCancelsItOnClose() {
        LegacyApplicationState state = readyState();
        RecordingScheduler scheduler = new RecordingScheduler();
        LegacyEffectRuntime runtime = runtime(state, scheduler);

        runtime.start();
        assertTrue(runtime.isRunning());
        assertEquals(40L, scheduler.delay);
        assertEquals(40L, scheduler.interval);
        assertEquals(7, runtime.getExecutorCount());
        assertEquals(1, runtime.getDefinitionCount());
    }

    @Test void closeCancelsTheRepeatingHandleAndIsIdempotent() {
        RecordingScheduler scheduler = new RecordingScheduler();
        LegacyEffectRuntime runtime = runtime(readyState(), scheduler);
        runtime.start();

        runtime.close();
        runtime.close();

        assertFalse(runtime.isRunning());
        assertEquals(1, scheduler.cancellations);
    }

    @Test void cannotConstructBeforeAuthoritativeStateIsReady() {
        LegacyApplicationState state = new LegacyApplicationState(storage());
        assertThrows(IllegalStateException.class, () -> runtime(state, new RecordingScheduler()));
    }

    private static LegacyEffectRuntime runtime(LegacyApplicationState state, RecordingScheduler scheduler) {
        FileConfiguration beacon = new YamlConfiguration();
        beacon.set("beacon.tick-interval", 40);
        FileConfiguration effects = new YamlConfiguration();
        effects.set("effects.crop_boost.enabled", true);
        effects.set("effects.crop_boost.type", "CROP_BOOST");
        effects.set("effects.crop_boost.max-level", 1);
        effects.set("effects.crop_boost.levels.1.growth-chance", 25);
        Plugin plugin = (Plugin) Proxy.newProxyInstance(Plugin.class.getClassLoader(),
                new Class<?>[] { Plugin.class }, (proxy, method, args) -> null);
        return new LegacyEffectRuntime(plugin, state, beacon, effects,
                new LegacyMaterialResolver(), new LegacyPotionEffectResolver(), scheduler);
    }

    private static LegacyApplicationState readyState() {
        LegacyApplicationState state = new LegacyApplicationState(storage());
        state.initialize();
        return state;
    }

    private static LegacyBeaconStorage storage() {
        return new LegacyBeaconStorage() {
            @Override public LegacyStorageLoadResult load() {
                return LegacyStorageLoadResult.success(Collections.<LegacyBeaconState>emptyList());
            }
            @Override public void store(Collection<LegacyBeaconState> beacons) { }
            @Override public void close() { }
            @Override public String getBackendName() { return "TEST"; }
        };
    }

    private static final class RecordingScheduler implements SchedulerService {
        private long delay;
        private long interval;
        private int cancellations;
        @Override public ScheduledTaskHandle runSyncTimer(Runnable runnable, long delayTicks, long intervalTicks) {
            delay = delayTicks;
            interval = intervalTicks;
            return new ScheduledTaskHandle() {
                private boolean cancelled;
                @Override public void cancel() {
                    if (!cancelled) cancellations++;
                    cancelled = true;
                }
            };
        }
        @Override public void runSync(Runnable runnable) { }
        @Override public void runSync(Location location, Runnable runnable) { }
        @Override public void runSync(Entity entity, Runnable runnable) { }
        @Override public ScheduledTaskHandle runSyncLater(Runnable runnable, long delayTicks) { return noTask(); }
        @Override public void runAsync(Runnable runnable) { }
        @Override public ScheduledTaskHandle runAsyncLater(Runnable runnable, long delayTicks) { return noTask(); }
        @Override public ScheduledTaskHandle runAsyncTimer(Runnable runnable, long delayTicks, long intervalTicks) {
            return noTask();
        }
        private ScheduledTaskHandle noTask() { return new ScheduledTaskHandle() { @Override public void cancel() { } }; }
    }
}
