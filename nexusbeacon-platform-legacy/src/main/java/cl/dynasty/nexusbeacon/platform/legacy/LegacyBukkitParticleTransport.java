package cl.dynasty.nexusbeacon.platform.legacy;

import java.lang.reflect.Method;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

final class LegacyBukkitParticleTransport implements LegacyParticleTransport {
    private final LegacyParticleRuntime runtime;

    LegacyBukkitParticleTransport(LegacyParticleRuntime runtime) {
        this.runtime = runtime;
    }

    @Override
    public void emit(Player player, LegacyParticleResolution resolution,
            LegacyParticleRequest request, int radius) {
        if (player == null || !sameWorld(player, request.getLocation())) return;
        if ("END_ROD".equals(resolution.getPhysicalName())) {
            spawnEndRod(player, request);
            return;
        }
        emitEffect(player, resolution.getEffect(), request, radius,
                "DUST".equals(resolution.getSemanticName()));
    }

    @Override
    public void emit(World world, LegacyParticleResolution resolution,
            LegacyParticleRequest request, int radius) {
        if (world == null || request.getLocation().getWorld() != world) return;
        if ("END_ROD".equals(resolution.getPhysicalName())) {
            spawnEndRod(world, request);
            return;
        }

        boolean coloredDust = "DUST".equals(resolution.getSemanticName()) && request.getColor() != null;
        boolean simple = !coloredDust && request.getOffsetX() == 0.0D && request.getOffsetY() == 0.0D
                && request.getOffsetZ() == 0.0D && request.getSpeed() == 0.0D;
        if (simple) {
            for (int i = 0; i < request.getAmount(); i++) {
                world.playEffect(request.getLocation(), resolution.getEffect(), 0, radius);
            }
            return;
        }

        for (Player player : world.getPlayers()) emit(player, resolution, request, radius);
    }

    private void emitEffect(Player player, Effect effect, LegacyParticleRequest request,
            int radius, boolean dust) {
        if (dust && request.getColor() != null) {
            LegacyParticleColor color = request.getColor();
            float red = color.getRed() == 0 ? Float.MIN_VALUE : color.getRed() / 255.0F;
            float green = color.getGreen() / 255.0F;
            float blue = color.getBlue() / 255.0F;
            for (int i = 0; i < request.getAmount(); i++) {
                player.spigot().playEffect(request.getLocation(), effect, 0, 0,
                        red, green, blue, 1.0F, 0, radius);
            }
            return;
        }

        player.spigot().playEffect(request.getLocation(), effect, 0, 0,
                (float) request.getOffsetX(), (float) request.getOffsetY(),
                (float) request.getOffsetZ(), (float) request.getSpeed(),
                request.getAmount(), radius);
    }

    private void spawnEndRod(Object audience, LegacyParticleRequest request) {
        if (!runtime.hasBukkitParticles()) {
            throw new IllegalStateException("END_ROD physical backend is unavailable on " + runtime.getRevision());
        }
        try {
            Class<?> particleClass = Class.forName("org.bukkit.Particle");
            @SuppressWarnings({ "rawtypes", "unchecked" })
            Object endRod = Enum.valueOf((Class<? extends Enum>) particleClass.asSubclass(Enum.class), "END_ROD");
            Method method = audience.getClass().getMethod("spawnParticle", particleClass,
                    Location.class, Integer.TYPE, Double.TYPE, Double.TYPE, Double.TYPE, Double.TYPE);
            method.invoke(audience, endRod, request.getLocation(), Integer.valueOf(request.getAmount()),
                    Double.valueOf(request.getOffsetX()), Double.valueOf(request.getOffsetY()),
                    Double.valueOf(request.getOffsetZ()), Double.valueOf(request.getSpeed()));
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not invoke the 1.12 END_ROD Bukkit API", exception);
        }
    }

    private static boolean sameWorld(Player player, Location location) {
        return location.getWorld() != null && location.getWorld().equals(player.getWorld());
    }
}
