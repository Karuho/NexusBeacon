package cl.dynasty.dynabeacon.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public final class LocationUtil {

    private LocationUtil() {
    }

    public static String serialize(Location location) {
        if (location == null || location.getWorld() == null) {
            return "";
        }

        return location.getWorld().getName()
                + ";" + location.getBlockX()
                + ";" + location.getBlockY()
                + ";" + location.getBlockZ();
    }

    public static Location deserialize(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        String[] parts = value.split(";");

        if (parts.length != 4) {
            return null;
        }

        World world = Bukkit.getWorld(parts[0]);

        if (world == null) {
            return null;
        }

        try {
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            int z = Integer.parseInt(parts[3]);

            return new Location(world, x, y, z);
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}