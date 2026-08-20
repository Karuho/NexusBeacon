package cl.dynasty.nexusbeacon.platform.legacy;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

import cl.dynasty.nexusbeacon.platform.api.ScheduledTaskHandle;
import cl.dynasty.nexusbeacon.platform.api.SchedulerService;

final class ImmediateScheduler implements SchedulerService {
    private static final ScheduledTaskHandle HANDLE = new ScheduledTaskHandle() {
        @Override public void cancel() { }
    };
    int syncCalls;

    @Override public void runSync(Runnable runnable) { syncCalls++; runnable.run(); }
    @Override public void runSync(Location location, Runnable runnable) { runSync(runnable); }
    @Override public void runSync(Entity entity, Runnable runnable) { runSync(runnable); }
    @Override public ScheduledTaskHandle runSyncLater(Runnable runnable, long delayTicks) { runSync(runnable); return HANDLE; }
    @Override public ScheduledTaskHandle runSyncTimer(Runnable runnable, long delayTicks, long intervalTicks) { runSync(runnable); return HANDLE; }
    @Override public void runAsync(Runnable runnable) { runnable.run(); }
    @Override public ScheduledTaskHandle runAsyncLater(Runnable runnable, long delayTicks) { runnable.run(); return HANDLE; }
    @Override public ScheduledTaskHandle runAsyncTimer(Runnable runnable, long delayTicks, long intervalTicks) { runnable.run(); return HANDLE; }
}
