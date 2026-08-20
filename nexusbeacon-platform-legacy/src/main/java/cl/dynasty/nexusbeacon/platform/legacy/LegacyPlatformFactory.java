package cl.dynasty.nexusbeacon.platform.legacy;

import org.bukkit.plugin.Plugin;

import cl.dynasty.nexusbeacon.platform.api.PlatformServices;
import cl.dynasty.nexusbeacon.platform.api.SchedulerService;

public final class LegacyPlatformFactory {
    private LegacyPlatformFactory() { }

    public static PlatformServices create(Plugin plugin) {
        SchedulerService scheduler = new LegacySchedulerService(plugin);
        return new PlatformServices(scheduler, new LegacyTeleporterService(scheduler));
    }
}
