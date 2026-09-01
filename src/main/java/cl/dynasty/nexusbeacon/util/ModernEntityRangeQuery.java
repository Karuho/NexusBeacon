package cl.dynasty.nexusbeacon.util;

import java.util.Collection;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;

public final class ModernEntityRangeQuery {
    private ModernEntityRangeQuery() {}

    public static Collection<Entity> nearby(Location center, int range) {
        World world = center.getWorld();
        if (world == null) throw new IllegalArgumentException("center world is required");
        double verticalRadius = verticalRadius(center.getY(), world.getMinHeight(), world.getMaxHeight());
        return world.getNearbyEntities(center, range, verticalRadius, range);
    }

    public static boolean isInsideHorizontal(Location location, Location center, int range) {
        if (location == null || center == null || location.getWorld() == null || center.getWorld() == null) {
            return false;
        }
        if (!location.getWorld().equals(center.getWorld())) return false;
        return isInsideHorizontal(location.getX() - center.getX(), location.getZ() - center.getZ(), range);
    }

    static double verticalRadius(double centerY, int minimumHeight, int maximumHeight) {
        return Math.max(centerY - minimumHeight, maximumHeight - centerY);
    }

    static boolean isInsideHorizontal(double dx, double dz, int range) {
        return (dx * dx) + (dz * dz) <= (double) range * range;
    }
}
