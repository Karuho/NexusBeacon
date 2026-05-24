package cl.dynasty.dynabeacon.service;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

public class ParticleService {

    public Particle resolve(String name) {
        if (name == null || name.trim().isEmpty()) {
            return Particle.HAPPY_VILLAGER;
        }

        String normalized = name.toUpperCase()
                .replace("VILLAGER_HAPPY", "HAPPY_VILLAGER")
                .replace(" ", "_")
                .replace("-", "_");

        try {
            return Particle.valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            return Particle.HAPPY_VILLAGER;
        }
    }

    public void spawn(Player player, String particleName, Location location, int amount,
                      double offsetX, double offsetY, double offsetZ, double speed) {
        if (player == null || location == null || location.getWorld() == null) {
            return;
        }

        player.spawnParticle(
                resolve(particleName),
                location,
                amount,
                offsetX,
                offsetY,
                offsetZ,
                speed
        );
    }
}