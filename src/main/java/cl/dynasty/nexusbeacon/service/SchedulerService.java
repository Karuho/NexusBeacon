package cl.dynasty.nexusbeacon.service;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

public interface SchedulerService {

    void runSync(Runnable runnable);

    void runSync(Location location, Runnable runnable);

    void runSync(Entity entity, Runnable runnable);

    ScheduledTaskHandle runSyncLater(Runnable runnable, long delayTicks);

    ScheduledTaskHandle runSyncTimer(Runnable runnable, long delayTicks, long intervalTicks);

    void runAsync(Runnable runnable);

    ScheduledTaskHandle runAsyncLater(Runnable runnable, long delayTicks);

    ScheduledTaskHandle runAsyncTimer(Runnable runnable, long delayTicks, long intervalTicks);
}