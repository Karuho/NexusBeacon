package cl.dynasty.nexusbeacon.platform.legacy;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import cl.dynasty.nexusbeacon.platform.api.ScheduledTaskHandle;
import cl.dynasty.nexusbeacon.platform.api.SchedulerService;

public final class LegacySchedulerService implements SchedulerService {
    private static final ScheduledTaskHandle NO_TASK = new ScheduledTaskHandle() {
        @Override public void cancel() { }
    };

    private final Plugin plugin;
    private final BukkitScheduler scheduler;

    public LegacySchedulerService(Plugin plugin) {
        this(plugin, plugin == null ? null : plugin.getServer().getScheduler());
    }

    LegacySchedulerService(Plugin plugin, BukkitScheduler scheduler) {
        if (plugin == null) throw new NullPointerException("plugin");
        if (scheduler == null) throw new NullPointerException("scheduler");
        this.plugin = plugin;
        this.scheduler = scheduler;
    }

    @Override
    public void runSync(Runnable runnable) {
        if (!canSchedule(runnable)) return;
        scheduler.runTask(plugin, runnable);
    }

    @Override public void runSync(Location location, Runnable runnable) { runSync(runnable); }
    @Override public void runSync(Entity entity, Runnable runnable) { runSync(runnable); }

    @Override
    public ScheduledTaskHandle runSyncLater(Runnable runnable, long delayTicks) {
        if (!canSchedule(runnable)) return NO_TASK;
        return handle(scheduler.runTaskLater(plugin, runnable, delayTicks));
    }

    @Override
    public ScheduledTaskHandle runSyncTimer(Runnable runnable, long delayTicks, long intervalTicks) {
        if (!canSchedule(runnable)) return NO_TASK;
        return handle(scheduler.runTaskTimer(plugin, runnable, delayTicks, intervalTicks));
    }

    @Override
    public void runAsync(Runnable runnable) {
        if (!canSchedule(runnable)) return;
        scheduler.runTaskAsynchronously(plugin, runnable);
    }

    @Override
    public ScheduledTaskHandle runAsyncLater(Runnable runnable, long delayTicks) {
        if (!canSchedule(runnable)) return NO_TASK;
        return handle(scheduler.runTaskLaterAsynchronously(plugin, runnable, delayTicks));
    }

    @Override
    public ScheduledTaskHandle runAsyncTimer(Runnable runnable, long delayTicks, long intervalTicks) {
        if (!canSchedule(runnable)) return NO_TASK;
        return handle(scheduler.runTaskTimerAsynchronously(plugin, runnable, delayTicks, intervalTicks));
    }

    private boolean canSchedule(Runnable runnable) {
        return plugin.isEnabled() && runnable != null;
    }

    private static ScheduledTaskHandle handle(BukkitTask task) {
        return new LegacyBukkitScheduledTaskHandle(task);
    }
}
