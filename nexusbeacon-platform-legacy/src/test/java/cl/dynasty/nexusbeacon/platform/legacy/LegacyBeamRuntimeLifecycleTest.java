package cl.dynasty.nexusbeacon.platform.legacy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.Collections;

import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;

class LegacyBeamRuntimeLifecycleTest {
    @Test void ownsExactlyOneRepeatingTaskAndClosesIdempotently() {
        LegacyApplicationState state = new LegacyApplicationState(new EmptyStorage());
        state.initialize();
        ImmediateScheduler scheduler = new ImmediateScheduler();
        LegacyParticleService particles = new LegacyParticleService(
                LegacyParticleRuntime.SPIGOT_1_8, scheduler, new RecordingParticleTransport());
        LegacyBeamRuntime runtime = new LegacyBeamRuntime(plugin(), state,
                new LegacyBeamRenderer(particles, scheduler), scheduler, new LegacyMaterialResolver(), 4L);

        runtime.start();
        assertTrue(runtime.isRunning());
        assertEquals(1, runtime.getRepeatingTaskCount());
        assertThrows(IllegalStateException.class, runtime::start);
        runtime.close();
        runtime.close();
        assertFalse(runtime.isRunning());
        assertEquals(0, runtime.getRepeatingTaskCount());
    }

    private static Plugin plugin() {
        return (Plugin) Proxy.newProxyInstance(Plugin.class.getClassLoader(), new Class<?>[] { Plugin.class },
                (proxy, method, args) -> null);
    }

    private static final class EmptyStorage implements LegacyBeaconStorage {
        @Override public LegacyStorageLoadResult load() {
            return LegacyStorageLoadResult.success(Collections.<LegacyBeaconState>emptyList());
        }
        @Override public void store(Collection<LegacyBeaconState> beacons) { }
        @Override public void close() { }
        @Override public String getBackendName() { return "TEST"; }
    }
}
