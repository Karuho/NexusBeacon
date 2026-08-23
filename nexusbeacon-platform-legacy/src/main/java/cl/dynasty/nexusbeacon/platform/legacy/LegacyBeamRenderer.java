package cl.dynasty.nexusbeacon.platform.legacy;

import java.util.function.BooleanSupplier;

import org.bukkit.Location;
import org.bukkit.World;

import cl.dynasty.nexusbeacon.platform.api.SchedulerService;
import cl.dynasty.nexusbeacon.platform.api.VerticalBeamGeometry;

public final class LegacyBeamRenderer {
    private final LegacyParticleService particles;
    private final LegacyBeamCompatibility compatibility;
    private final SchedulerService scheduler;

    public LegacyBeamRenderer(LegacyParticleService particles, SchedulerService scheduler) {
        if (particles == null) throw new NullPointerException("particles");
        if (scheduler == null) throw new NullPointerException("scheduler");
        this.particles = particles;
        this.compatibility = new LegacyBeamCompatibility(particles);
        this.scheduler = scheduler;
    }

    LegacyBeamRenderResult render(final Location base, int configuredHeight,
            double configuredStep, int configuredCount, final LegacyBeamStylePlan style) {
        return render(base, configuredHeight, configuredStep, configuredCount, style,
                new BooleanSupplier() { @Override public boolean getAsBoolean() { return true; } });
    }

    LegacyBeamRenderResult render(final Location base, int configuredHeight,
            double configuredStep, int configuredCount, final LegacyBeamStylePlan style,
            final BooleanSupplier authority) {
        if (base == null || base.getWorld() == null) {
            throw new IllegalArgumentException("beam base must have a world");
        }
        if (authority == null) throw new NullPointerException("authority");
        final int height = Math.max(1, configuredHeight);
        final double step = Math.max(0.25D, configuredStep);
        final int count = Math.max(1, configuredCount);
        final LegacyBeamStyleCompatibility styleCompatibility = compatibility.classify(style);
        final int points = VerticalBeamGeometry.pointCount(height, step);
        if (styleCompatibility.getStatus() == LegacyBeamCompatibilityStatus.UNSUPPORTED) {
            return new LegacyBeamRenderResult(styleCompatibility, points, false);
        }

        scheduler.runSync(base, new Runnable() {
            @Override public void run() {
                if (!authority.getAsBoolean()) return;
                final World world = base.getWorld();
                final LegacyParticleResolution resolution = styleCompatibility.getParticle();
                VerticalBeamGeometry.forEachPoint(base, height, step, point -> {
                    LegacyParticleRequest request = new LegacyParticleRequest(
                            style.getParticleName(), point, count, 0.0D, 0.0D, 0.0D, 0.0D,
                            style.getColor(), style.getSize());
                    particles.emitToWorldNow(world, resolution, request);
                });
            }
        });
        return new LegacyBeamRenderResult(styleCompatibility, points, true);
    }
}
