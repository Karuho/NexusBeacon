package cl.dynasty.nexusbeacon.platform.legacy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.concurrent.atomic.AtomicInteger;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.Test;

import cl.dynasty.nexusbeacon.platform.api.ScheduledTaskHandle;

class LegacySchedulerServiceTest {
    @Test void routesAllSyncOperationsToBukkitTickScheduler() {
        Fixture fixture = new Fixture(true);
        Runnable runnable = new Runnable() { @Override public void run() { } };

        fixture.service.runSync(runnable);
        fixture.service.runSync((Location) null, runnable);
        fixture.service.runSync((Entity) null, runnable);

        assertEquals(3, fixture.scheduler.calls);
        assertEquals("runTask", fixture.scheduler.lastMethod);
        assertSame(runnable, fixture.scheduler.lastArguments[1]);
    }

    @Test void preservesSyncDelayAndTimerTicks() {
        Fixture fixture = new Fixture(true);
        Runnable runnable = new Runnable() { @Override public void run() { } };

        fixture.service.runSyncLater(runnable, 7L);
        assertEquals("runTaskLater", fixture.scheduler.lastMethod);
        assertEquals(7L, fixture.scheduler.lastArguments[2]);

        fixture.service.runSyncTimer(runnable, 3L, 11L);
        assertEquals("runTaskTimer", fixture.scheduler.lastMethod);
        assertEquals(3L, fixture.scheduler.lastArguments[2]);
        assertEquals(11L, fixture.scheduler.lastArguments[3]);
    }

    @Test void routesExplicitAsyncOperationsOnlyToAsyncSchedulerMethods() {
        Fixture fixture = new Fixture(true);
        Runnable runnable = new Runnable() { @Override public void run() { } };

        fixture.service.runAsync(runnable);
        assertEquals("runTaskAsynchronously", fixture.scheduler.lastMethod);
        fixture.service.runAsyncLater(runnable, 5L);
        assertEquals("runTaskLaterAsynchronously", fixture.scheduler.lastMethod);
        fixture.service.runAsyncTimer(runnable, 2L, 9L);
        assertEquals("runTaskTimerAsynchronously", fixture.scheduler.lastMethod);
        assertEquals(2L, fixture.scheduler.lastArguments[2]);
        assertEquals(9L, fixture.scheduler.lastArguments[3]);
    }

    @Test void taskHandleCancellationIsIdempotent() {
        Fixture fixture = new Fixture(true);
        ScheduledTaskHandle handle = fixture.service.runSyncLater(new Runnable() {
            @Override public void run() { }
        }, 1L);

        handle.cancel();
        handle.cancel();

        assertEquals(1, fixture.task.cancelCalls);
    }

    @Test void cancelledRepeatingTaskCannotExecuteAgainInSchedulerHarness() {
        Fixture fixture = new Fixture(true);
        final AtomicInteger executions = new AtomicInteger();
        ScheduledTaskHandle handle = fixture.service.runSyncTimer(new Runnable() {
            @Override public void run() { executions.incrementAndGet(); }
        }, 0L, 1L);

        fixture.scheduler.runLastIfActive();
        handle.cancel();
        fixture.scheduler.runLastIfActive();

        assertEquals(1, executions.get());
    }

    @Test void disabledPluginAndNullWorkCannotCreateTasks() {
        Fixture fixture = new Fixture(false);

        fixture.service.runSync(new Runnable() { @Override public void run() { } });
        fixture.service.runAsync(new Runnable() { @Override public void run() { } });
        fixture.service.runSyncLater(null, 1L).cancel();
        fixture.service.runSyncTimer(null, 1L, 1L).cancel();
        fixture.service.runAsyncLater(null, 1L).cancel();
        fixture.service.runAsyncTimer(null, 1L, 1L).cancel();

        assertEquals(0, fixture.scheduler.calls);
        assertEquals(0, fixture.task.cancelCalls);
    }

    private static final class Fixture {
        private final SchedulerRecorder scheduler = new SchedulerRecorder();
        private final TaskRecorder task = scheduler.task;
        private final LegacySchedulerService service;

        private Fixture(final boolean enabled) {
            Plugin plugin = (Plugin) Proxy.newProxyInstance(Plugin.class.getClassLoader(),
                    new Class<?>[] { Plugin.class }, new InvocationHandler() {
                        @Override public Object invoke(Object proxy, Method method, Object[] args) {
                            if (method.getName().equals("isEnabled")) return enabled;
                            return defaultValue(method.getReturnType());
                        }
                    });
            service = new LegacySchedulerService(plugin, scheduler.proxy);
        }
    }

    private static final class SchedulerRecorder implements InvocationHandler {
        private int calls;
        private String lastMethod;
        private Object[] lastArguments;
        private final TaskRecorder task = new TaskRecorder();
        private final BukkitScheduler proxy = (BukkitScheduler) Proxy.newProxyInstance(
                BukkitScheduler.class.getClassLoader(), new Class<?>[] { BukkitScheduler.class }, this);

        @Override public Object invoke(Object proxy, Method method, Object[] args) {
            if (method.getName().startsWith("runTask")) {
                calls++;
                lastMethod = method.getName();
                lastArguments = args;
                task.cancelled = false;
                return task.proxy;
            }
            return defaultValue(method.getReturnType());
        }

        private void runLastIfActive() {
            if (!task.cancelled) ((Runnable) lastArguments[1]).run();
        }
    }

    private static final class TaskRecorder implements InvocationHandler {
        private int cancelCalls;
        private boolean cancelled;
        private final BukkitTask proxy = (BukkitTask) Proxy.newProxyInstance(
                BukkitTask.class.getClassLoader(), new Class<?>[] { BukkitTask.class }, this);

        @Override public Object invoke(Object proxy, Method method, Object[] args) {
            if (method.getName().equals("cancel")) {
                cancelCalls++;
                cancelled = true;
            }
            return defaultValue(method.getReturnType());
        }
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
