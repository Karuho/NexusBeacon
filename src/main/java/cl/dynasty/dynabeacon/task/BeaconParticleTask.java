package cl.dynasty.dynabeacon.task;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import cl.dynasty.dynabeacon.DynaBeaconPlugin;
import cl.dynasty.dynabeacon.model.BeaconData;
import cl.dynasty.dynabeacon.model.PlayerSettings;

public class BeaconParticleTask extends BukkitRunnable {

    private final DynaBeaconPlugin plugin;

    public BeaconParticleTask(DynaBeaconPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        if (!plugin.getConfigManager().getBeaconConfig().getBoolean("beacon.particles.enabled", true)) {
            return;
        }

        int points = plugin.getConfigManager().getBeaconConfig().getInt("beacon.particles.radius-points", 128);

        for (BeaconData beacon : plugin.getBeaconManager().getBeacons()) {
            Location center = beacon.getLocation();

            if (center == null || center.getWorld() == null) {
                continue;
            }

            int range = beacon.getRange();

            for (Player player : center.getWorld().getPlayers()) {
                PlayerSettings settings = plugin.getPlayerSettingsManager().get(player.getUniqueId());

                if (!settings.isShowParticle()) {
                    continue;
                }

                if (player.getLocation().distanceSquared(center) > (range + 48) * (range + 48)) {
                    continue;
                }

                drawCircle(player, center, range, points);
            }
        }
    }

    private void drawCircle(Player player, Location center, int radius, int points) {
        String particleName = plugin.getPlayerSettingsManager()
                .get(player.getUniqueId())
                .getParticleType();

        org.bukkit.Particle particle;

        try {
            particle = org.bukkit.Particle.valueOf(particleName.toUpperCase());
        } catch (Exception exception) {
            particle = org.bukkit.Particle.HAPPY_VILLAGER;
        }

        for (int i = 0; i < points; i++) {
            double angle = 2.0D * Math.PI * i / points;
            double x = center.getBlockX() + 0.5D + Math.cos(angle) * radius;
            double z = center.getBlockZ() + 0.5D + Math.sin(angle) * radius;
            double y = center.getBlockY() + 1.0D;

            Location location = new Location(center.getWorld(), x, y, z);

            player.spawnParticle(particle, location, 2, 0.05D, 0.05D, 0.05D, 0.0D);
        }
    }
}