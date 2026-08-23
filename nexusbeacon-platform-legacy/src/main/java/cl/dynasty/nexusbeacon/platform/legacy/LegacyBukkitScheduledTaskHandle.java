package cl.dynasty.nexusbeacon.platform.legacy;

import java.util.concurrent.atomic.AtomicBoolean;

import org.bukkit.scheduler.BukkitTask;

import cl.dynasty.nexusbeacon.platform.api.ScheduledTaskHandle;

public final class LegacyBukkitScheduledTaskHandle implements ScheduledTaskHandle {
    private final BukkitTask task;
    private final AtomicBoolean cancelled = new AtomicBoolean();

    public LegacyBukkitScheduledTaskHandle(BukkitTask task) {
        if (task == null) throw new NullPointerException("task");
        this.task = task;
    }

    @Override
    public void cancel() {
        if (cancelled.compareAndSet(false, true)) task.cancel();
    }
}
