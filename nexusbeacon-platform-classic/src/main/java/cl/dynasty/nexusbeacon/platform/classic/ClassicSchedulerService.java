package cl.dynasty.nexusbeacon.platform.classic;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import cl.dynasty.nexusbeacon.platform.api.ScheduledTaskHandle;
import cl.dynasty.nexusbeacon.platform.api.SchedulerService;

public final class ClassicSchedulerService implements SchedulerService {
    private static final ScheduledTaskHandle NO_TASK = new ScheduledTaskHandle() { public void cancel() { } };
    private final Plugin plugin; private final BukkitScheduler scheduler;
    public ClassicSchedulerService(Plugin plugin) {
        if (plugin == null) throw new NullPointerException("plugin");
        this.plugin = plugin; this.scheduler = plugin.getServer().getScheduler();
    }
    private boolean valid(Runnable runnable) { return plugin.isEnabled() && runnable != null; }
    private static ScheduledTaskHandle handle(final BukkitTask task) { return new ScheduledTaskHandle() { public void cancel() { task.cancel(); } }; }
    public void runSync(Runnable task) { if (valid(task)) scheduler.runTask(plugin, task); }
    public void runSync(Location location, Runnable task) { runSync(task); }
    public void runSync(Entity entity, Runnable task) { runSync(task); }
    public ScheduledTaskHandle runSyncLater(Runnable task, long delay) { return valid(task) ? handle(scheduler.runTaskLater(plugin, task, delay)) : NO_TASK; }
    public ScheduledTaskHandle runSyncTimer(Runnable task, long delay, long period) { return valid(task) ? handle(scheduler.runTaskTimer(plugin, task, delay, period)) : NO_TASK; }
    public void runAsync(Runnable task) { if (valid(task)) scheduler.runTaskAsynchronously(plugin, task); }
    public ScheduledTaskHandle runAsyncLater(Runnable task, long delay) { return valid(task) ? handle(scheduler.runTaskLaterAsynchronously(plugin, task, delay)) : NO_TASK; }
    public ScheduledTaskHandle runAsyncTimer(Runnable task, long delay, long period) { return valid(task) ? handle(scheduler.runTaskTimerAsynchronously(plugin, task, delay, period)) : NO_TASK; }
}
