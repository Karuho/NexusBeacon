package cl.dynasty.nexusbeacon.platform.modern;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.player.PlayerTeleportEvent;

import cl.dynasty.nexusbeacon.platform.api.SchedulerService;
import cl.dynasty.nexusbeacon.platform.api.TeleporterService;

public final class ModernBukkitTeleporterService implements TeleporterService {
    private final SchedulerService scheduler;

    public ModernBukkitTeleporterService(SchedulerService scheduler) { this.scheduler = scheduler; }

    @Override
    public void teleport(Entity entity, Location location, PlayerTeleportEvent.TeleportCause cause) {
        if (entity == null || location == null) return;
        scheduler.runSync(entity, () -> entity.teleport(location, cause));
    }
}
