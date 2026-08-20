package cl.dynasty.nexusbeacon.platform.api;

import java.util.function.Consumer;

import org.bukkit.Location;

/** Shared, version-neutral sampling for the current straight vertical beam. */
public final class VerticalBeamGeometry {
    private VerticalBeamGeometry() { }

    public static void forEachPoint(Location base, int height, double step, Consumer<Location> consumer) {
        if (base == null) throw new NullPointerException("base");
        if (consumer == null) throw new NullPointerException("consumer");
        if (height < 1) throw new IllegalArgumentException("height must be at least 1");
        if (step <= 0.0D) throw new IllegalArgumentException("step must be positive");

        for (double y = 0.0D; y <= height; y += step) {
            consumer.accept(base.clone().add(0.0D, y, 0.0D));
        }
    }

    public static int pointCount(int height, double step) {
        if (height < 1) throw new IllegalArgumentException("height must be at least 1");
        if (step <= 0.0D) throw new IllegalArgumentException("step must be positive");

        int count = 0;
        for (double y = 0.0D; y <= height; y += step) count++;
        return count;
    }
}
