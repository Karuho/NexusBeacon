package cl.dynasty.nexusbeacon.platform.legacy;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.player.PlayerTeleportEvent;

import cl.dynasty.nexusbeacon.platform.api.SchedulerService;
import cl.dynasty.nexusbeacon.platform.api.TeleporterService;

public final class LegacyTeleporterService implements TeleporterService {
    private final SchedulerService scheduler;

    public LegacyTeleporterService(SchedulerService scheduler) {
        if (scheduler == null) throw new NullPointerException("scheduler");
        this.scheduler = scheduler;
    }

    @Override
    public void teleport(final Entity entity, final Location location,
            final PlayerTeleportEvent.TeleportCause cause) {
        if (entity == null || location == null) return;
        scheduler.runSync(entity, new Runnable() {
            @Override public void run() { entity.teleport(location, cause); }
        });
    }
}
