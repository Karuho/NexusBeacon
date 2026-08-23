package cl.dynasty.nexusbeacon.platform.legacy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

class LegacyBukkitParticleTransportTest {
    @Test void sharedPlayerParticleUsesVerifiedSpigotParameters() {
        World world = world(new WorldRecorder());
        SpigotRecorder spigot = new SpigotRecorder();
        Player player = player(world, spigot);
        LegacyParticleRequest request = new LegacyParticleRequest("FLAME",
                new Location(world, 1, 2, 3), 2, 0.05D, 0.06D, 0.07D, 0.2D, null, 1.0F);

        new LegacyBukkitParticleTransport(LegacyParticleRuntime.SPIGOT_1_8).emit(
                player, service().resolve("FLAME"), request, 46340);

        assertEquals(1, spigot.calls);
        assertEquals(Effect.FLAME, spigot.effect);
        assertEquals(2, spigot.count);
        assertEquals(0.05F, spigot.offsetX);
        assertEquals(0.2F, spigot.speed);
        assertEquals(46340, spigot.radius);
    }

    @Test void coloredDustUsesHistoricalRgbEncodingAndOnePacketPerRequestedParticle() {
        World world = world(new WorldRecorder());
        SpigotRecorder spigot = new SpigotRecorder();
        Player player = player(world, spigot);
        LegacyParticleRequest request = new LegacyParticleRequest("DUST",
                new Location(world, 1, 2, 3), 2, 0, 0, 0, 0,
                new LegacyParticleColor(0, 255, 128), 1.2F);

        new LegacyBukkitParticleTransport(LegacyParticleRuntime.SPIGOT_1_8).emit(
                player, service().resolve("DUST"), request, 512);

        assertEquals(2, spigot.calls);
        assertEquals(Effect.COLOURED_DUST, spigot.effect);
        assertEquals(Float.MIN_VALUE, spigot.offsetX);
        assertEquals(1.0F, spigot.offsetY);
        assertEquals(128.0F / 255.0F, spigot.offsetZ);
        assertEquals(1.0F, spigot.speed);
        assertEquals(0, spigot.count);
    }

    @Test void simpleWorldEmissionUsesBukkitWorldEffectAndRadius() {
        WorldRecorder recorder = new WorldRecorder();
        World world = world(recorder);
        LegacyParticleRequest request = new LegacyParticleRequest("PORTAL",
                new Location(world, 1, 2, 3), 2, 0, 0, 0, 0, null, 1.0F);

        new LegacyBukkitParticleTransport(LegacyParticleRuntime.SPIGOT_1_8).emit(
                world, service().resolve("PORTAL"), request, 512);

        assertEquals(2, recorder.playEffectCalls);
        assertSame(Effect.PORTAL, recorder.effect);
        assertEquals(512, recorder.radius);
    }

    private static LegacyParticleService service() {
        return LegacyParticleResolutionTest.service(LegacyParticleRuntime.SPIGOT_1_8);
    }

    private static Player player(final World world, final SpigotRecorder spigot) {
        return (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[] { Player.class },
                (proxy, method, args) -> {
                    if ("getWorld".equals(method.getName())) return world;
                    if ("spigot".equals(method.getName())) return spigot;
                    return defaultValue(method.getReturnType());
                });
    }

    private static World world(final WorldRecorder recorder) {
        return (World) Proxy.newProxyInstance(World.class.getClassLoader(), new Class<?>[] { World.class }, recorder);
    }

    private static final class WorldRecorder implements InvocationHandler {
        private int playEffectCalls;
        private Effect effect;
        private int radius;

        @Override public Object invoke(Object proxy, Method method, Object[] args) {
            if ("equals".equals(method.getName())) return proxy == args[0];
            if ("getPlayers".equals(method.getName())) return Collections.emptyList();
            if ("playEffect".equals(method.getName()) && args.length == 4) {
                playEffectCalls++;
                effect = (Effect) args[1];
                radius = ((Integer) args[3]).intValue();
            }
            return defaultValue(method.getReturnType());
        }
    }

    private static final class SpigotRecorder extends Player.Spigot {
        private int calls;
        private Effect effect;
        private float offsetX;
        private float offsetY;
        private float offsetZ;
        private float speed;
        private int count;
        private int radius;

        @Override public void playEffect(Location location, Effect effect, int id, int data,
                float offsetX, float offsetY, float offsetZ, float speed, int count, int radius) {
            calls++;
            this.effect = effect;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.offsetZ = offsetZ;
            this.speed = speed;
            this.count = count;
            this.radius = radius;
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
