package cl.dynasty.dynabeacon.util;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;

public final class MobUtil {

    private MobUtil() {
    }

    public static boolean isHostile(Entity entity) {
        if (!(entity instanceof LivingEntity)) return false;
        if (entity instanceof Monster) return true;

        String type = entity.getType().name();

        return type.equalsIgnoreCase("PHANTOM")
                || type.equalsIgnoreCase("SLIME")
                || type.equalsIgnoreCase("MAGMA_CUBE")
                || type.equalsIgnoreCase("GHAST")
                || type.equalsIgnoreCase("SHULKER");
    }
}