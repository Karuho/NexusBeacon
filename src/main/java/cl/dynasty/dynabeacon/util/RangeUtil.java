package cl.dynasty.dynabeacon.util;

import org.bukkit.Location;

public final class RangeUtil {

    private RangeUtil() {
    }

    public static boolean isInsideHorizontalRange(Location location, Location center, int range) {
        if (location == null || center == null) return false;
        if (location.getWorld() == null || center.getWorld() == null) return false;
        if (!location.getWorld().equals(center.getWorld())) return false;

        double dx = location.getX() - center.getX();
        double dz = location.getZ() - center.getZ();

        return (dx * dx) + (dz * dz) <= range * range;
    }
}