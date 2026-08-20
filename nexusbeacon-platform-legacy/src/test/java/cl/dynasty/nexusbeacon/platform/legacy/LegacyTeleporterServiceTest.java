package cl.dynasty.nexusbeacon.platform.legacy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.junit.jupiter.api.Test;

import cl.dynasty.nexusbeacon.platform.api.ScheduledTaskHandle;
import cl.dynasty.nexusbeacon.platform.api.SchedulerService;

class LegacyTeleporterServiceTest {
    @Test void schedulesSuccessfulTeleportOnEntitySyncContext() {
        RecordingScheduler scheduler = new RecordingScheduler(true);
        RecordingEntity entity = new RecordingEntity(false, true);
        Location destination = destination(12.25, 64.5, -9.75, 137.5F, -21.25F);

        new LegacyTeleporterService(scheduler).teleport(
                entity.proxy, destination, PlayerTeleportEvent.TeleportCause.PLUGIN);

        assertSame(entity.proxy, scheduler.entity);
        assertEquals(1, scheduler.syncEntityCalls);
        assertEquals(1, entity.teleports);
        assertSame(destination, entity.destination);
        assertEquals(PlayerTeleportEvent.TeleportCause.PLUGIN, entity.cause);
        assertEquals(137.5F, entity.destination.getYaw());
        assertEquals(-21.25F, entity.destination.getPitch());
    }

    @Test void preservesCrossWorldDestinationObject() {
        RecordingScheduler scheduler = new RecordingScheduler(true);
        RecordingEntity entity = new RecordingEntity(false, true);
        Location destination = destination(1, 2, 3, 4, 5);

        new LegacyTeleporterService(scheduler).teleport(
                entity.proxy, destination, PlayerTeleportEvent.TeleportCause.COMMAND);

        assertSame(destination.getWorld(), entity.destination.getWorld());
        assertSame(destination, entity.destination);
    }

    @Test void bukkitFalseResultIsNotInventedAsSuccess() {
        RecordingScheduler scheduler = new RecordingScheduler(true);
        RecordingEntity entity = new RecordingEntity(false, false);

        new LegacyTeleporterService(scheduler).teleport(
                entity.proxy, destination(0, 0, 0, 0, 0), PlayerTeleportEvent.TeleportCause.PLUGIN);

        assertEquals(1, entity.teleports);
        assertEquals(false, entity.lastResult);
    }

    @Test void offThreadStyleInvocationQueuesWithoutBlockingOrTouchingEntity() {
        RecordingScheduler scheduler = new RecordingScheduler(false);
        RecordingEntity entity = new RecordingEntity(false, true);

        new LegacyTeleporterService(scheduler).teleport(
                entity.proxy, destination(0, 1, 2, 3, 4), PlayerTeleportEvent.TeleportCause.PLUGIN);

        assertEquals(1, scheduler.syncEntityCalls);
        assertEquals(0, entity.teleports);
        scheduler.queued.run();
        assertEquals(1, entity.teleports);
    }

    @Test void ignoresNullEntityOrDestination() {
        RecordingScheduler scheduler = new RecordingScheduler(true);
        LegacyTeleporterService teleporter = new LegacyTeleporterService(scheduler);
        teleporter.teleport(null, destination(0, 0, 0, 0, 0), PlayerTeleportEvent.TeleportCause.PLUGIN);
        teleporter.teleport(new RecordingEntity(false, true).proxy, null, PlayerTeleportEvent.TeleportCause.PLUGIN);
        assertEquals(0, scheduler.syncEntityCalls);
    }

    @Test void entityExceptionPropagatesFromTheScheduledOperation() {
        RecordingScheduler scheduler = new RecordingScheduler(true);
        RecordingEntity entity = new RecordingEntity(true, false);
        assertThrows(IllegalStateException.class, () -> new LegacyTeleporterService(scheduler).teleport(
                entity.proxy, destination(0, 0, 0, 0, 0), PlayerTeleportEvent.TeleportCause.PLUGIN));
    }

    private static Location destination(double x, double y, double z, float yaw, float pitch) {
        World world = (World) Proxy.newProxyInstance(World.class.getClassLoader(),
                new Class<?>[] { World.class }, (proxy, method, args) -> defaultValue(method.getReturnType()));
        return new Location(world, x, y, z, yaw, pitch);
    }

    private static final class RecordingEntity implements InvocationHandler {
        private int teleports;
        private Location destination;
        private PlayerTeleportEvent.TeleportCause cause;
        private boolean lastResult;
        private final boolean throwsFailure;
        private final boolean result;
        private final Entity proxy = (Entity) Proxy.newProxyInstance(
                Entity.class.getClassLoader(), new Class<?>[] { Entity.class }, this);

        private RecordingEntity(boolean throwsFailure, boolean result) {
            this.throwsFailure = throwsFailure;
            this.result = result;
        }

        @Override public Object invoke(Object proxy, Method method, Object[] args) {
            if (method.getName().equals("teleport")) {
                if (throwsFailure) throw new IllegalStateException("teleport failed");
                teleports++;
                destination = (Location) args[0];
                cause = (PlayerTeleportEvent.TeleportCause) args[1];
                lastResult = result;
                return result;
            }
            return defaultValue(method.getReturnType());
        }
    }

    private static final class RecordingScheduler implements SchedulerService {
        private final boolean immediate;
        private Entity entity;
        private Runnable queued;
        private int syncEntityCalls;
        private static final ScheduledTaskHandle HANDLE = new ScheduledTaskHandle() {
            @Override public void cancel() { }
        };

        private RecordingScheduler(boolean immediate) { this.immediate = immediate; }
        @Override public void runSync(Runnable runnable) { if (immediate) runnable.run(); else queued = runnable; }
        @Override public void runSync(Location location, Runnable runnable) { runSync(runnable); }
        @Override public void runSync(Entity entity, Runnable runnable) {
            this.entity = entity;
            syncEntityCalls++;
            runSync(runnable);
        }
        @Override public ScheduledTaskHandle runSyncLater(Runnable runnable, long delayTicks) { return HANDLE; }
        @Override public ScheduledTaskHandle runSyncTimer(Runnable runnable, long delayTicks, long intervalTicks) { return HANDLE; }
        @Override public void runAsync(Runnable runnable) { runnable.run(); }
        @Override public ScheduledTaskHandle runAsyncLater(Runnable runnable, long delayTicks) { return HANDLE; }
        @Override public ScheduledTaskHandle runAsyncTimer(Runnable runnable, long delayTicks, long intervalTicks) { return HANDLE; }
    }

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
