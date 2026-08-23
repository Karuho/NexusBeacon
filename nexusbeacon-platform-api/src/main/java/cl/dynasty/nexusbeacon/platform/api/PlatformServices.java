package cl.dynasty.nexusbeacon.platform.api;

import java.util.Objects;

public final class PlatformServices {
    private final SchedulerService scheduler;
    private final TeleporterService teleporter;

    public PlatformServices(SchedulerService scheduler, TeleporterService teleporter) {
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        this.teleporter = Objects.requireNonNull(teleporter, "teleporter");
    }

    public SchedulerService getScheduler() { return scheduler; }
    public TeleporterService getTeleporter() { return teleporter; }
}
