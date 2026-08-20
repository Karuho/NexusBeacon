package cl.dynasty.nexusbeacon.platform.modern;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.player.PlayerTeleportEvent;

import cl.dynasty.nexusbeacon.NexusBeaconPlugin;
import cl.dynasty.nexusbeacon.platform.api.PlatformServices;
import cl.dynasty.nexusbeacon.platform.api.SchedulerService;
import cl.dynasty.nexusbeacon.platform.api.TeleporterService;

public final class ModernPlatformFactory {
    private ModernPlatformFactory() {}

    public static PlatformServices create(NexusBeaconPlugin plugin) {
        boolean foliaScheduler = hasFoliaScheduler();
        SchedulerService scheduler = createScheduler(plugin, foliaScheduler);
        plugin.getLogger().info(foliaScheduler
                ? "Scheduler active: Folia"
                : "Scheduler active: Bukkit/Paper classic");

        boolean paperTeleport = hasPaperAsyncTeleport();
        TeleporterService teleporter = createTeleporter(scheduler, paperTeleport);
        plugin.getLogger().info(paperTeleport
                ? "Teleporter active: Paper async"
                : "Teleporter active: Bukkit sync");

        return new PlatformServices(scheduler, teleporter);
    }

    static PlatformServices create(NexusBeaconPlugin plugin, boolean foliaScheduler, boolean paperTeleport) {
        SchedulerService scheduler = createScheduler(plugin, foliaScheduler);
        return new PlatformServices(scheduler, createTeleporter(scheduler, paperTeleport));
    }

    private static SchedulerService createScheduler(NexusBeaconPlugin plugin, boolean foliaScheduler) {
        return foliaScheduler
                ? new ModernFoliaSchedulerService(plugin)
                : new ModernBukkitSchedulerService(plugin);
    }

    private static TeleporterService createTeleporter(SchedulerService scheduler, boolean paperTeleport) {
        return paperTeleport
                ? new ModernPaperTeleporterService(scheduler)
                : new ModernBukkitTeleporterService(scheduler);
    }

    private static boolean hasFoliaScheduler() {
        try {
            Class.forName("io.papermc.paper.threadedregions.scheduler.ScheduledTask");
            return true;
        } catch (ClassNotFoundException exception) {
            return false;
        }
    }

    private static boolean hasPaperAsyncTeleport() {
        try {
            Entity.class.getMethod("teleportAsync", Location.class, PlayerTeleportEvent.TeleportCause.class);
            return true;
        } catch (NoSuchMethodException exception) {
            return false;
        }
    }
}
