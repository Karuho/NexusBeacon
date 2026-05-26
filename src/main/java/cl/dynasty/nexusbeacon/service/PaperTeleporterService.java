package cl.dynasty.nexusbeacon.service;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.player.PlayerTeleportEvent;

public class PaperTeleporterService implements TeleporterService {

    private final SchedulerService schedulerService;

    public PaperTeleporterService(SchedulerService schedulerService) {
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
                entity.teleportAsync(location, cause);
            }
        });
    }
}