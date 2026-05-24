package cl.dynasty.dynabeacon.service;

import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

import cl.dynasty.dynabeacon.DynaBeaconPlugin;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

public class FoliaSchedulerService implements SchedulerService {

    private final DynaBeaconPlugin plugin;

    public FoliaSchedulerService(DynaBeaconPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void runSync(Runnable runnable) {
        if (!plugin.isEnabled()) {
            return;
        }

        Bukkit.getGlobalRegionScheduler().execute(plugin, runnable);
    }

    @Override
    public void runSync(Location location, Runnable runnable) {
        if (!plugin.isEnabled()) {
            return;
        }

        if (location == null) {
            runSync(runnable);
            return;
        }

        Bukkit.getRegionScheduler().execute(plugin, location, runnable);
    }

    @Override
    public void runSync(Entity entity, Runnable runnable) {
        if (!plugin.isEnabled()) {
            return;
        }

        if (entity == null) {
            runSync(runnable);
            return;
        }

        entity.getScheduler().run(plugin, task -> runnable.run(), null);
    }

    @Override
    public ScheduledTaskHandle runSyncLater(Runnable runnable, long delayTicks) {
        ScheduledTask task = Bukkit.getGlobalRegionScheduler()
                .runDelayed(plugin, scheduledTask -> runnable.run(), delayTicks);

        return new FoliaScheduledTaskHandle(task);
    }

    @Override
    public ScheduledTaskHandle runSyncTimer(Runnable runnable, long delayTicks, long intervalTicks) {
        ScheduledTask task = Bukkit.getGlobalRegionScheduler()
                .runAtFixedRate(plugin, scheduledTask -> runnable.run(), delayTicks, intervalTicks);

        return new FoliaScheduledTaskHandle(task);
    }

    @Override
    public void runAsync(Runnable runnable) {
        if (!plugin.isEnabled()) {
            return;
        }

        Bukkit.getAsyncScheduler().runNow(plugin, scheduledTask -> runnable.run());
    }

    @Override
    public ScheduledTaskHandle runAsyncLater(Runnable runnable, long delayTicks) {
        ScheduledTask task = Bukkit.getAsyncScheduler()
                .runDelayed(plugin, scheduledTask -> runnable.run(), delayTicks * 50L, TimeUnit.MILLISECONDS);

        return new FoliaScheduledTaskHandle(task);
    }

    @Override
    public ScheduledTaskHandle runAsyncTimer(Runnable runnable, long delayTicks, long intervalTicks) {
        ScheduledTask task = Bukkit.getAsyncScheduler()
                .runAtFixedRate(
                        plugin,
                        scheduledTask -> runnable.run(),
                        delayTicks * 50L,
                        intervalTicks * 50L,
                        TimeUnit.MILLISECONDS);

        return new FoliaScheduledTaskHandle(task);
    }

    private static class FoliaScheduledTaskHandle implements ScheduledTaskHandle {

        private final ScheduledTask task;

        private FoliaScheduledTaskHandle(ScheduledTask task) {
            this.task = task;
        }

        @Override
        public void cancel() {
            task.cancel();
        }
    }
}