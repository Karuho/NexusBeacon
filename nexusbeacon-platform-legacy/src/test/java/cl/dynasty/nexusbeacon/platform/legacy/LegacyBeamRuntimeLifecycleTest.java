package cl.dynasty.nexusbeacon.platform.legacy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.UUID;

import org.bukkit.Material;

import org.bukkit.Location;
import org.bukkit.World;
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
                new LegacyBeamRenderer(particles, scheduler), scheduler, new LegacyMaterialResolver(),
                policy(), 4L);

        runtime.start();
        assertTrue(runtime.isRunning());
        assertEquals(1, runtime.getRepeatingTaskCount());
        assertThrows(IllegalStateException.class, runtime::start);
        runtime.close();
        runtime.close();
        assertFalse(runtime.isRunning());
        assertEquals(0, runtime.getRepeatingTaskCount());
    }

    @Test void reconstructsDefaultAndPersistedCustomStylesWithoutMutation() {
        LegacyApplicationState state = new LegacyApplicationState(new EmptyStorage());
        state.initialize();
        ImmediateScheduler scheduler = new ImmediateScheduler();
        LegacyParticleService particles = new LegacyParticleService(
                LegacyParticleRuntime.SPIGOT_1_8, scheduler, new RecordingParticleTransport());
        LegacyBeamRuntime runtime = new LegacyBeamRuntime(plugin(), state,
                new LegacyBeamRenderer(particles, scheduler), scheduler, new LegacyMaterialResolver(),
                policy(), 4L);

        assertEquals("aqua", runtime.resolveStyle(null).getId());
        assertEquals("purple", runtime.resolveStyle("purple").getId());
        assertEquals(0, state.size());
    }

    @Test void rendererRejectsEmissionWhenManagedAuthorityWasRevoked() {
        ImmediateScheduler scheduler = new ImmediateScheduler();
        RecordingParticleTransport transport = new RecordingParticleTransport();
        LegacyParticleService particles = new LegacyParticleService(
                LegacyParticleRuntime.SPIGOT_1_8, scheduler, transport);
        LegacyBeamRenderer renderer = new LegacyBeamRenderer(particles, scheduler);
        World world = (World) Proxy.newProxyInstance(World.class.getClassLoader(),
                new Class<?>[] { World.class }, (proxy, method, args) -> null);

        renderer.render(new Location(world, 0.5D, 65.0D, 0.5D), 8, 1.0D, 1,
                LegacyBeamStylePlan.defaultStyle(), () -> false);

        assertEquals(0, transport.worldCalls);
    }

    @Test void unmanagedRemovedAndStaleBeaconsCannotPassRuntimeAuthorityGate() {
        LegacyApplicationState state = new LegacyApplicationState(new EmptyStorage());
        state.initialize();
        ImmediateScheduler scheduler = new ImmediateScheduler();
        LegacyParticleService particles = new LegacyParticleService(
                LegacyParticleRuntime.SPIGOT_1_8, scheduler, new RecordingParticleTransport());
        LegacyBeamRuntime runtime = new LegacyBeamRuntime(plugin(), state,
                new LegacyBeamRenderer(particles, scheduler), scheduler, new LegacyMaterialResolver(),
                policy(), 4L);
        LegacyBeaconState original = beacon(24);
        World customBase = customBaseWorld();

        assertFalse(runtime.isAuthoritativeRenderCandidate(original, customBase));
        assertTrue(state.insert(original));
        assertTrue(runtime.isAuthoritativeRenderCandidate(original, customBase));
        LegacyBeaconState updated = beacon(48);
        assertTrue(state.update(updated));
        assertFalse(runtime.isAuthoritativeRenderCandidate(original, customBase));
        assertTrue(runtime.isAuthoritativeRenderCandidate(updated, customBase));
        assertTrue(state.delete(updated.getLocation()));
        assertFalse(runtime.isAuthoritativeRenderCandidate(updated, customBase));
    }

    private static Plugin plugin() {
        return (Plugin) Proxy.newProxyInstance(Plugin.class.getClassLoader(), new Class<?>[] { Plugin.class },
                (proxy, method, args) -> null);
    }

    private static LegacyBeamRenderPolicy policy() {
        return new LegacyBeamRenderPolicy("AUTO", 4,
                EnumSet.of(Material.IRON_BLOCK, Material.REDSTONE_BLOCK));
    }

    private static LegacyBeaconState beacon(int range) {
        return new LegacyBeaconState(new LegacyBeaconLocation("world", 0, 64, 0),
                UUID.fromString("11111111-1111-4111-8111-111111111111"),
                UUID.fromString("22222222-2222-4222-8222-222222222222"), range, 1,
                new LinkedHashMap<String, Integer>(), new LinkedHashSet<String>(),
                new LinkedHashSet<UUID>(), true, "aqua", true, "VILLAGER_HAPPY");
    }

    private static World customBaseWorld() {
        return (World) Proxy.newProxyInstance(World.class.getClassLoader(), new Class<?>[] { World.class },
                (proxy, method, args) -> {
                    if (!"getBlockAt".equals(method.getName())) return null;
                    return Proxy.newProxyInstance(org.bukkit.block.Block.class.getClassLoader(),
                            new Class<?>[] { org.bukkit.block.Block.class },
                            (block, blockMethod, blockArgs) -> "getType".equals(blockMethod.getName())
                                    ? Material.REDSTONE_BLOCK : null);
                });
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
