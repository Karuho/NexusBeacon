package cl.dynasty.nexusbeacon.service;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitTask;

import cl.dynasty.nexusbeacon.NexusBeaconPlugin;

public class SpigotSchedulerService implements SchedulerService {

    private final NexusBeaconPlugin plugin;

    public SpigotSchedulerService(NexusBeaconPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void runSync(Runnable runnable) {
        if (!plugin.isEnabled() || runnable == null) {
            return;
        }

        plugin.getServer().getScheduler().runTask(plugin, runnable);
    }

    @Override
    public void runSync(Location location, Runnable runnable) {
        runSync(runnable);
    }

    @Override
    public void runSync(Entity entity, Runnable runnable) {
        runSync(runnable);
    }

    @Override
    public ScheduledTaskHandle runSyncLater(Runnable runnable, long delayTicks) {
        BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, runnable, delayTicks);
        return new BukkitScheduledTaskHandle(task);
    }

    @Override
    public ScheduledTaskHandle runSyncTimer(Runnable runnable, long delayTicks, long intervalTicks) {
        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, runnable, delayTicks, intervalTicks);
        return new BukkitScheduledTaskHandle(task);
    }

    @Override
    public void runAsync(Runnable runnable) {
        if (!plugin.isEnabled()) {
            return;
        }

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, runnable);
    }

    @Override
    public ScheduledTaskHandle runAsyncLater(Runnable runnable, long delayTicks) {
        BukkitTask task = plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, runnable, delayTicks);
        return new BukkitScheduledTaskHandle(task);
    }

    @Override
    public ScheduledTaskHandle runAsyncTimer(Runnable runnable, long delayTicks, long intervalTicks) {
        BukkitTask task = plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, runnable, delayTicks,
                intervalTicks);
        return new BukkitScheduledTaskHandle(task);
    }

    private static class BukkitScheduledTaskHandle implements ScheduledTaskHandle {

        private final BukkitTask task;

        private BukkitScheduledTaskHandle(BukkitTask task) {
            this.task = task;
        }

        @Override
        public void cancel() {
            task.cancel();
        }
    }
}