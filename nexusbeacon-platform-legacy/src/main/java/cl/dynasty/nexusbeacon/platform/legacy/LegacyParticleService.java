package cl.dynasty.nexusbeacon.platform.legacy;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.bukkit.Effect;
import org.bukkit.World;
import org.bukkit.entity.Player;

import cl.dynasty.nexusbeacon.platform.api.SchedulerService;

public final class LegacyParticleService {
    private static final int WORLD_RADIUS = 512;
    private static final int PLAYER_RADIUS = 46340;
    private static final Set<String> KNOWN_MODERN_ONLY = new HashSet<String>(Arrays.asList(
            "BLOCK_MARKER", "DUST_COLOR_TRANSITION", "SCULK_CHARGE", "SHRIEK",
            "SONIC_BOOM", "TRIAL_SPAWNER_DETECTION"));

    private final LegacyParticleRuntime runtime;
    private final SchedulerService scheduler;
    private final LegacyParticleTransport transport;

    public LegacyParticleService(LegacyParticleRuntime runtime, SchedulerService scheduler) {
        this(runtime, scheduler, new LegacyBukkitParticleTransport(runtime));
    }

    LegacyParticleService(LegacyParticleRuntime runtime, SchedulerService scheduler,
            LegacyParticleTransport transport) {
        if (runtime == null) throw new NullPointerException("runtime");
        if (scheduler == null) throw new NullPointerException("scheduler");
        if (transport == null) throw new NullPointerException("transport");
        this.runtime = runtime;
        this.scheduler = scheduler;
        this.transport = transport;
    }

    public LegacyParticleRuntime getRuntime() { return runtime; }

    public LegacyParticleResolution resolve(String configuredName) {
        String name = normalize(configuredName);
        if (name == null) return invalid(configuredName);
        if ("HAPPY_VILLAGER".equals(name)) {
            return resolution("VILLAGER_HAPPY", "HAPPY_VILLAGER",
                    LegacyParticleCompatibility.LEGACY_ALIAS, Effect.HAPPY_VILLAGER, false, true);
        }
        if ("VILLAGER_HAPPY".equals(name)) return effect(name, Effect.HAPPY_VILLAGER);
        if ("FLAME".equals(name)) return effect(name, Effect.FLAME);
        if ("CRIT".equals(name)) return effect(name, Effect.CRIT);
        if ("CLOUD".equals(name)) return effect(name, Effect.CLOUD);
        if ("PORTAL".equals(name)) return effect(name, Effect.PORTAL);
        if ("DUST".equals(name) || "REDSTONE".equals(name)) {
            return resolution("DUST", "COLOURED_DUST", LegacyParticleCompatibility.LEGACY_ALIAS,
                    Effect.COLOURED_DUST, true, false);
        }
        if ("END_ROD".equals(name)) {
            if (runtime.hasBukkitParticles()) {
                return resolution(name, name, LegacyParticleCompatibility.EXACT, null, false, true);
            }
            return resolution(name, "FIREWORKS_SPARK",
                    LegacyParticleCompatibility.VISUAL_APPROXIMATION,
                    Effect.FIREWORKS_SPARK, false, true);
        }
        if (KNOWN_MODERN_ONLY.contains(name)) {
            return resolution(name, null, LegacyParticleCompatibility.UNSUPPORTED, null, false, false);
        }
        return invalid(configuredName);
    }

    public LegacyParticleResolution emitToPlayer(final Player player, final LegacyParticleRequest request) {
        final LegacyParticleResolution resolution = resolve(request == null ? null : request.getParticleName());
        if (player == null || request == null || !resolution.isSupported()) return resolution;
        scheduler.runSync(request.getLocation(), new Runnable() {
            @Override public void run() { transport.emit(player, resolution, request, PLAYER_RADIUS); }
        });
        return resolution;
    }

    public LegacyParticleResolution emitToWorld(final World world, final LegacyParticleRequest request) {
        final LegacyParticleResolution resolution = resolve(request == null ? null : request.getParticleName());
        if (world == null || request == null || !resolution.isSupported()) return resolution;
        scheduler.runSync(request.getLocation(), new Runnable() {
            @Override public void run() { transport.emit(world, resolution, request, WORLD_RADIUS); }
        });
        return resolution;
    }

    void emitToWorldNow(World world, LegacyParticleResolution resolution, LegacyParticleRequest request) {
        transport.emit(world, resolution, request, WORLD_RADIUS);
    }

    private LegacyParticleResolution effect(String name, Effect effect) {
        return resolution(name, effect.name(), LegacyParticleCompatibility.LEGACY_EFFECT_EQUIVALENT,
                effect, false, true);
    }

    private static LegacyParticleResolution invalid(String name) {
        return resolution(name, null, LegacyParticleCompatibility.INVALID, null, false, false);
    }

    private static LegacyParticleResolution resolution(String semantic, String physical,
            LegacyParticleCompatibility compatibility, Effect effect,
            boolean colorSupported, boolean sizeSupported) {
        return new LegacyParticleResolution(semantic, physical, compatibility, effect,
                colorSupported, sizeSupported);
    }

    private static String normalize(String name) {
        if (name == null || name.trim().isEmpty()) return null;
        return name.trim().toUpperCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
    }
}
