package cl.dynasty.nexusbeacon.platform.legacy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Proxy;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

class LegacyParticleServiceTest {
    @Test void handsPlayerEmissionToSyncSchedulerWithoutDroppingParameters() {
        ImmediateScheduler scheduler = new ImmediateScheduler();
        RecordingParticleTransport transport = new RecordingParticleTransport();
        LegacyParticleService service = new LegacyParticleService(
                LegacyParticleRuntime.SPIGOT_1_8, scheduler, transport);
        LegacyParticleRequest request = request("FLAME");

        service.emitToPlayer(proxy(Player.class), request);

        assertEquals(1, scheduler.syncCalls);
        assertEquals(1, transport.playerCalls);
        assertEquals(46340, transport.lastRadius);
        assertEquals(2, transport.lastRequest.getAmount());
        assertEquals(0.05D, transport.lastRequest.getOffsetX());
    }

    @Test void handsWorldEmissionToSyncSchedulerWithLongDistanceRadius() {
        ImmediateScheduler scheduler = new ImmediateScheduler();
        RecordingParticleTransport transport = new RecordingParticleTransport();
        LegacyParticleService service = new LegacyParticleService(
                LegacyParticleRuntime.SPIGOT_1_8, scheduler, transport);

        service.emitToWorld(proxy(World.class), request("PORTAL"));

        assertEquals(1, scheduler.syncCalls);
        assertEquals(1, transport.worldCalls);
        assertEquals(512, transport.lastRadius);
    }

    @Test void invalidAndUnsupportedRequestsNeverReachSchedulerOrTransport() {
        ImmediateScheduler scheduler = new ImmediateScheduler();
        RecordingParticleTransport transport = new RecordingParticleTransport();
        LegacyParticleService service = new LegacyParticleService(
                LegacyParticleRuntime.SPIGOT_1_8, scheduler, transport);
        Player player = proxy(Player.class);

        service.emitToPlayer(player, request("SONIC_BOOM"));
        service.emitToPlayer(player, request("BOGUS"));

        assertEquals(0, scheduler.syncCalls);
        assertEquals(0, transport.playerCalls);
    }

    private static LegacyParticleRequest request(String name) {
        return new LegacyParticleRequest(name, new Location(null, 1.0D, 2.0D, 3.0D), 2,
                0.05D, 0.06D, 0.07D, 0.2D, null, 1.0F);
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type },
                (instance, method, args) -> defaultValue(method.getReturnType()));
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
