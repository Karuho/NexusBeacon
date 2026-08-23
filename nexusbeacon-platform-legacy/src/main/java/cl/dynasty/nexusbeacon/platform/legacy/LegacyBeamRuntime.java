package cl.dynasty.nexusbeacon.platform.legacy;

import java.util.Collection;
import java.util.List;
import java.util.function.BooleanSupplier;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import cl.dynasty.nexusbeacon.platform.api.MaterialContext;
import cl.dynasty.nexusbeacon.platform.api.ScheduledTaskHandle;
import cl.dynasty.nexusbeacon.platform.api.SchedulerService;

/** One global repeating task renders current authoritative beam styles. */
public final class LegacyBeamRuntime implements Runnable {
    private final Plugin plugin;
    private final LegacyApplicationState state;
    private final LegacyBeamRenderer renderer;
    private final SchedulerService scheduler;
    private final LegacyBeamRenderPolicy renderPolicy;
    private final List<LegacyBeamStylePlan> styles;
    private final Material beaconMaterial;
    private final long intervalTicks;
    private ScheduledTaskHandle task;
    private boolean running;

    public LegacyBeamRuntime(Plugin plugin, LegacyApplicationState state, LegacyBeamRenderer renderer,
            SchedulerService scheduler, LegacyMaterialResolver materials,
            LegacyBeamRenderPolicy renderPolicy, long intervalTicks) {
        if (plugin == null || state == null || renderer == null || scheduler == null || materials == null
                || renderPolicy == null) {
            throw new NullPointerException();
        }
        this.plugin = plugin;
        this.state = state;
        this.renderer = renderer;
        this.scheduler = scheduler;
        this.renderPolicy = renderPolicy;
        this.styles = LegacyBeamStylePlan.currentDefaults();
        this.beaconMaterial = materials.resolveMaterial("BEACON", MaterialContext.BLOCK_MATCH)
                .getMaterial().orElse(Material.BEACON);
        this.intervalTicks = Math.max(1L, intervalTicks);
    }

    public synchronized void start() {
        if (running) throw new IllegalStateException("Legacy beam runtime is already running");
        running = true;
        task = scheduler.runSyncTimer(this, intervalTicks, intervalTicks);
    }

    @Override public void run() {
        if (!running || !state.getStatus().isReady()) return;
        Collection<LegacyBeaconState> beacons = state.snapshot();
        for (LegacyBeaconState beacon : beacons) {
            LegacyBeaconLocation stored = beacon.getLocation();
            World world = plugin.getServer().getWorld(stored.getWorldName());
            if (world == null || !world.isChunkLoaded(stored.getX() >> 4, stored.getZ() >> 4)
                    || world.getBlockAt(stored.getX(), stored.getY(), stored.getZ()).getType() != beaconMaterial) {
                continue;
            }
            if (!isAuthoritativeRenderCandidate(beacon, world)) continue;
            LegacyBeamStylePlan style = resolveStyle(beacon.getBeamStyle());
            if (style == null) continue;
            final LegacyBeaconState authority = beacon;
            renderer.render(new Location(world, stored.getX() + 0.5D, stored.getY() + 1.0D,
                    stored.getZ() + 0.5D), 96, 0.45D, 1, style,
                    new BooleanSupplier() {
                        @Override public boolean getAsBoolean() {
                            return running && state.isAuthoritative(authority);
                        }
                    });
        }
    }

    public synchronized void close() {
        running = false;
        if (task != null) task.cancel();
        task = null;
    }

    public synchronized boolean isRunning() { return running; }
    public synchronized int getRepeatingTaskCount() { return task == null ? 0 : 1; }

    boolean isAuthoritativeRenderCandidate(LegacyBeaconState beacon, World world) {
        return state.isAuthoritative(beacon)
                && renderPolicy.shouldRender(world, beacon == null ? null : beacon.getLocation());
    }

    LegacyBeamStylePlan resolveStyle(String id) {
        if (id == null) return LegacyBeamStylePlan.defaultStyle();
        for (LegacyBeamStylePlan style : styles) if (style.getId().equalsIgnoreCase(id)) return style;
        return null;
    }
}
