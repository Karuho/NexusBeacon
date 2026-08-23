package cl.dynasty.nexusbeacon.platform.modern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.reflect.Proxy;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.junit.jupiter.api.Test;

import cl.dynasty.nexusbeacon.platform.api.ScheduledTaskHandle;
import cl.dynasty.nexusbeacon.platform.api.SchedulerService;

class ModernTeleporterServiceTest {
    @Test
    void paperTeleporterDelegatesThroughEntitySchedulerAndKeepsAsyncCall() {
        RecordingEntity entity = new RecordingEntity();
        ImmediateScheduler scheduler = new ImmediateScheduler();
        Location destination = new Location(null, 1, 2, 3);

        new ModernPaperTeleporterService(scheduler)
                .teleport(entity.proxy, destination, PlayerTeleportEvent.TeleportCause.PLUGIN);

        assertSame(entity.proxy, scheduler.entity);
        assertEquals(1, scheduler.entityRuns);
        assertEquals(1, entity.asyncTeleports);
        assertEquals(0, entity.syncTeleports);
        assertSame(destination, entity.destination);
    }

    @Test
    void bukkitTeleporterDelegatesThroughEntitySchedulerAndKeepsSyncCall() {
        RecordingEntity entity = new RecordingEntity();
        ImmediateScheduler scheduler = new ImmediateScheduler();
        Location destination = new Location(null, 4, 5, 6);

        new ModernBukkitTeleporterService(scheduler)
                .teleport(entity.proxy, destination, PlayerTeleportEvent.TeleportCause.PLUGIN);

        assertSame(entity.proxy, scheduler.entity);
        assertEquals(1, scheduler.entityRuns);
        assertEquals(0, entity.asyncTeleports);
        assertEquals(1, entity.syncTeleports);
        assertSame(destination, entity.destination);
    }

    @Test
    void teleportersIgnoreNullInputsWithoutScheduling() {
        ImmediateScheduler scheduler = new ImmediateScheduler();
        Location destination = new Location(null, 0, 0, 0);
        new ModernPaperTeleporterService(scheduler).teleport(null, destination, PlayerTeleportEvent.TeleportCause.PLUGIN);
        new ModernBukkitTeleporterService(scheduler).teleport(null, destination, PlayerTeleportEvent.TeleportCause.PLUGIN);
        assertEquals(0, scheduler.entityRuns);
    }

    private static final class RecordingEntity {
        private int asyncTeleports;
        private int syncTeleports;
        private Location destination;
        private final Entity proxy = (Entity) Proxy.newProxyInstance(
                Entity.class.getClassLoader(), new Class<?>[] { Entity.class }, (ignored, method, arguments) -> {
                    if (method.getName().equals("teleportAsync")) {
                        asyncTeleports++;
                        destination = (Location) arguments[0];
                        return CompletableFuture.completedFuture(Boolean.TRUE);
                    }
                    if (method.getName().equals("teleport")) {
                        syncTeleports++;
                        destination = (Location) arguments[0];
                        return Boolean.TRUE;
                    }
                    if (method.getName().equals("toString")) return "RecordingEntity";
                    return defaultValue(method.getReturnType());
                });

        private static Object defaultValue(Class<?> type) {
            if (!type.isPrimitive()) return null;
            if (type == boolean.class) return false;
            if (type == char.class) return '\0';
            if (type == byte.class) return (byte) 0;
            if (type == short.class) return (short) 0;
            if (type == int.class) return 0;
            if (type == long.class) return 0L;
            if (type == float.class) return 0F;
            return 0D;
        }
    }

    private static final class ImmediateScheduler implements SchedulerService {
        private Entity entity;
        private int entityRuns;
        private static final ScheduledTaskHandle HANDLE = () -> {};

        @Override public void runSync(Runnable runnable) { runnable.run(); }
        @Override public void runSync(Location location, Runnable runnable) { runnable.run(); }
        @Override public void runSync(Entity entity, Runnable runnable) {
            this.entity = entity;
            entityRuns++;
            runnable.run();
        }
        @Override public ScheduledTaskHandle runSyncLater(Runnable runnable, long delayTicks) { return HANDLE; }
        @Override public ScheduledTaskHandle runSyncTimer(Runnable runnable, long delayTicks, long intervalTicks) { return HANDLE; }
        @Override public void runAsync(Runnable runnable) { runnable.run(); }
        @Override public ScheduledTaskHandle runAsyncLater(Runnable runnable, long delayTicks) { return HANDLE; }
        @Override public ScheduledTaskHandle runAsyncTimer(Runnable runnable, long delayTicks, long intervalTicks) { return HANDLE; }
    }
}
