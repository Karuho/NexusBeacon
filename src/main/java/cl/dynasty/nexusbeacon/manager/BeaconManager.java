package cl.dynasty.nexusbeacon.manager;

import cl.dynasty.nexusbeacon.NexusBeaconPlugin;
import cl.dynasty.nexusbeacon.model.BeaconData;
import cl.dynasty.nexusbeacon.util.LocationUtil;
import org.bukkit.Location;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BeaconManager {

    private final NexusBeaconPlugin plugin;
    private final Map<String, BeaconData> beacons = new HashMap<String, BeaconData>();

    public BeaconManager(NexusBeaconPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        beacons.clear();

        for (BeaconData beacon : plugin.getStorageManager().loadBeacons()) {
            beacon.setRange(plugin.getBeaconPowerManager().calculateRange(beacon));
            beacons.put(beacon.getId(), beacon);
        }

        plugin.getLogger().info("Beacons cargados: " + beacons.size());
    }

    public BeaconData registerBeacon(Location location, UUID owner) {
        String id = LocationUtil.serialize(location);
        String uniqueId = UUID.randomUUID().toString();

        int range = plugin.getConfigManager().getBeaconConfig().getInt("beacon.default-range", 100);

        BeaconData beacon = new BeaconData(id, uniqueId, location, owner, range, 1, null, null, null, true, null);
        beacon.setRange(plugin.getBeaconPowerManager().calculateRange(beacon));
        beacons.put(id, beacon);
        plugin.getStorageManager().saveBeacon(beacon);

        return beacon;
    }

    public BeaconData registerBeacon(Location location, UUID owner, String uniqueId,
            Map<String, Integer> effects,
            java.util.Set<String> activeEffects) {
        String id = LocationUtil.serialize(location);

        int range = plugin.getConfigManager().getBeaconConfig().getInt("beacon.default-range", 100);

        BeaconData beacon = new BeaconData(id, uniqueId, location, owner, range, 1, effects, activeEffects, null, true,
                null);
        beacon.setRange(plugin.getBeaconPowerManager().calculateRange(beacon));
        beacons.put(id, beacon);
        plugin.getStorageManager().saveBeacon(beacon);

        return beacon;
    }

    public void removeBeacon(Location location) {
        String id = LocationUtil.serialize(location);
        beacons.remove(id);
        plugin.getStorageManager().removeBeacon(id);
    }

    public BeaconData getBeacon(Location location) {
        return beacons.get(LocationUtil.serialize(location));
    }

    public BeaconData getBeacon(String id) {
        return beacons.get(id);
    }

    public BeaconData getBeaconById(String id) {
        return beacons.get(id);
    }

    public Collection<BeaconData> getBeacons() {
        return beacons.values();
    }

    public BeaconData getNearestBeacon(Location location) {
        if (location == null || location.getWorld() == null) {
            return null;
        }

        BeaconData nearest = null;
        double nearestDistanceSquared = Double.MAX_VALUE;

        for (BeaconData beacon : beacons.values()) {
            if (beacon == null || beacon.getLocation() == null || beacon.getLocation().getWorld() == null) {
                continue;
            }

            if (!beacon.getLocation().getWorld().equals(location.getWorld())) {
                continue;
            }

            double distanceSquared = beacon.getLocation().distanceSquared(location);

            if (distanceSquared > beacon.getRange() * beacon.getRange()) {
                continue;
            }

            if (distanceSquared < nearestDistanceSquared) {
                nearest = beacon;
                nearestDistanceSquared = distanceSquared;
            }
        }

        return nearest;
    }
}