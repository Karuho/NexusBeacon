package cl.dynasty.dynabeacon.service;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.player.PlayerTeleportEvent;

public class SpigotTeleporterService implements TeleporterService {

    private final SchedulerService schedulerService;

    public SpigotTeleporterService(SchedulerService schedulerService) {
        this.schedulerService = schedulerService;
    }

    @Override
    public void teleport(Entity entity, Location location, PlayerTeleportEvent.TeleportCause cause) {
        if (entity == null || location == null) {
            return;
        }

        schedulerService.runSync(entity, new Runnable() {
            @Override
            public void run() {
                entity.teleport(location, cause);
            }
        });
    }
}