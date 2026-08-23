package cl.dynasty.nexusbeacon.platform.legacy;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import cl.dynasty.nexusbeacon.platform.api.MaterialContext;
import cl.dynasty.nexusbeacon.platform.api.ScheduledTaskHandle;
import cl.dynasty.nexusbeacon.platform.api.SchedulerService;

/** Renders the persisted NexusBeacon range circle to nearby players. */
public final class LegacyRangeParticleRuntime implements Runnable {
    private final Plugin plugin;
    private final LegacyApplicationState state;
    private final LegacyParticleService particles;
    private final SchedulerService scheduler;
    private final Material beaconMaterial;
    private final int points;
    private final long intervalTicks;
    private ScheduledTaskHandle task;
    private boolean running;

    public LegacyRangeParticleRuntime(Plugin plugin, LegacyApplicationState state,
            LegacyParticleService particles, SchedulerService scheduler,
            LegacyMaterialResolver materials, int points, long intervalTicks) {
        if (plugin == null || state == null || particles == null || scheduler == null || materials == null) {
            throw new NullPointerException();
        }
        this.plugin = plugin;
        this.state = state;
        this.particles = particles;
        this.scheduler = scheduler;
        this.beaconMaterial = materials.resolveMaterial("BEACON", MaterialContext.BLOCK_MATCH)
                .getMaterial().orElse(Material.BEACON);
        this.points = Math.max(8, points);
        this.intervalTicks = Math.max(1L, intervalTicks);
    }

    public synchronized void start() {
        if (running) throw new IllegalStateException("Legacy range runtime is already running");
        running = true;
        task = scheduler.runSyncTimer(this, intervalTicks, intervalTicks);
    }

    @Override public void run() {
        if (!running || !state.getStatus().isReady()) return;
        for (LegacyBeaconState beacon : state.snapshot()) {
            if (!state.isAuthoritative(beacon) || !beacon.isRangeParticlesEnabled()) continue;
            LegacyBeaconLocation stored = beacon.getLocation();
            World world = plugin.getServer().getWorld(stored.getWorldName());
            if (world == null || !world.isChunkLoaded(stored.getX() >> 4, stored.getZ() >> 4)
                    || world.getBlockAt(stored.getX(), stored.getY(), stored.getZ()).getType() != beaconMaterial) {
                continue;
            }
            LegacyParticleResolution resolution = particles.resolve(beacon.getRangeParticleType());
            if (!resolution.isSupported()) continue;
            Location center = new Location(world, stored.getX() + 0.5D, stored.getY() + 1.0D,
                    stored.getZ() + 0.5D);
            double visibleRadius = beacon.getRange() + 48.0D;
            for (Player player : world.getPlayers()) {
                if (!state.isAuthoritative(beacon)) break;
                if (player.getLocation().distanceSquared(center) > visibleRadius * visibleRadius) continue;
                renderCircle(player, center, beacon.getRange(), beacon.getRangeParticleType(), resolution);
            }
        }
    }

    private void renderCircle(Player player, Location center, int radius, String particleType,
            LegacyParticleResolution resolution) {
        for (int index = 0; index < points; index++) {
            double angle = 2.0D * Math.PI * index / points;
            Location point = center.clone().add(Math.cos(angle) * radius, 0.0D, Math.sin(angle) * radius);
            LegacyParticleRequest request = new LegacyParticleRequest(particleType, point, 2,
                    0.05D, 0.05D, 0.05D, 0.0D, null, 1.0F);
            particles.emitToPlayerNow(player, resolution, request);
        }
    }

    public synchronized void close() {
        running = false;
        if (task != null) task.cancel();
        task = null;
    }

    public synchronized boolean isRunning() { return running; }
    public synchronized int getRepeatingTaskCount() { return task == null ? 0 : 1; }
}
