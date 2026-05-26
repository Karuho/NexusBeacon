package cl.dynasty.nexusbeacon.task;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import cl.dynasty.nexusbeacon.NexusBeaconPlugin;
import cl.dynasty.nexusbeacon.model.BeaconData;

public class BeaconParticleTask implements Runnable {

    private final NexusBeaconPlugin plugin;

    public BeaconParticleTask(NexusBeaconPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        if (!plugin.getConfigManager().getBeaconConfig().getBoolean("beacon.particles.enabled", true)) {
            return;
        }

        int points = plugin.getConfigManager().getBeaconConfig().getInt("beacon.particles.radius-points", 128);

        for (BeaconData beacon : plugin.getBeaconManager().getBeacons()) {
            if (!beacon.isRangeParticlesEnabled()) {
                continue;
            }

            Location center = beacon.getLocation();

            if (center == null || center.getWorld() == null) {
                continue;
            }

            int range = beacon.getRange();

            for (Player player : center.getWorld().getPlayers()) {
                if (player.getLocation().distanceSquared(center) > (range + 48) * (range + 48)) {
                    continue;
                }

                drawCircle(player, beacon, center, range, points);
            }
        }
    }

    private void drawCircle(Player player, BeaconData beacon, Location center, int radius, int points) {
        String particleName = beacon.getRangeParticleType();

        if (particleName == null || particleName.isBlank()) {
            particleName = plugin.getConfigManager().getBeaconConfig()
                    .getString("beacon.particles.type", "VILLAGER_HAPPY");
        }

        for (int i = 0; i < points; i++) {
            double angle = 2.0D * Math.PI * i / points;
            double x = center.getBlockX() + 0.5D + Math.cos(angle) * radius;
            double z = center.getBlockZ() + 0.5D + Math.sin(angle) * radius;
            double y = center.getBlockY() + 1.0D;

            Location location = new Location(center.getWorld(), x, y, z);

            plugin.getParticleService().spawn(
                    player,
                    particleName,
                    location,
                    2,
                    0.05D,
                    0.05D,
                    0.05D,
                    0.0D);
        }
    }
}