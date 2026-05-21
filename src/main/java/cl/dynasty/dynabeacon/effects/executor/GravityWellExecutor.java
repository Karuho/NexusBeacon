package cl.dynasty.dynabeacon.effects.executor;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import cl.dynasty.dynabeacon.DynaBeaconPlugin;
import cl.dynasty.dynabeacon.effects.BeaconEffect;
import cl.dynasty.dynabeacon.model.BeaconData;
import cl.dynasty.dynabeacon.util.MobUtil;
import cl.dynasty.dynabeacon.util.RangeUtil;

public class GravityWellExecutor implements EffectExecutor {

    private final DynaBeaconPlugin plugin;

    public GravityWellExecutor(DynaBeaconPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getType() {
        return "GRAVITY_WELL";
    }

    @Override
    public void tick(BeaconData beacon, BeaconEffect effect) {
        Location center = beacon.getLocation();
        if (center == null || center.getWorld() == null)
            return;

        ConfigurationSection section = plugin.getConfigManager()
                .getEffectsConfig()
                .getConfigurationSection("effects." + effect.getId());

        int level = beacon.getEffectLevel(effect.getId());
        double strength = section != null ? section.getDouble("pull-strength-per-level", 0.08D) * Math.max(1, level)
                : 0.08D;
        double verticalBoost = section != null ? section.getDouble("vertical-boost", 0.05D) : 0.05D;

        Location target = center.clone().add(0.5D, 1.0D, 0.5D);

        for (Entity entity : center.getWorld().getEntities()) {
            if (!(entity instanceof LivingEntity))
                continue;
            if (!MobUtil.isHostile(entity))
                continue;
            if (!RangeUtil.isInsideHorizontalRange(entity.getLocation(), center, beacon.getRange()))
                continue;

            Vector direction = target.toVector().subtract(entity.getLocation().toVector());

            if (direction.lengthSquared() <= 0.01D)
                continue;

            direction.normalize().multiply(strength);
            direction.setY(verticalBoost);

            entity.setVelocity(entity.getVelocity().add(direction));
            org.bukkit.util.Vector velocity = entity.getVelocity().add(direction);

            double maxVelocity = section != null ? section.getDouble("max-velocity", 1.2D) : 1.2D;
            if (velocity.length() > maxVelocity) {
                velocity.normalize().multiply(maxVelocity);
            }

            entity.setVelocity(velocity);
        }
    }
}