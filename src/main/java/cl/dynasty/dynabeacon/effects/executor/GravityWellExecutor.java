package cl.dynasty.dynabeacon.effects.executor;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import cl.dynasty.dynabeacon.DynaBeaconPlugin;
import cl.dynasty.dynabeacon.effects.BeaconEffect;
import cl.dynasty.dynabeacon.effects.EffectLevelUtil;
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

        int level = Math.max(1, beacon.getEffectLevel(effect.getId()));
        double pullStrengthPerLevel = section != null ? section.getDouble("pull-strength-per-level", 0.08D) : 0.08D;
        double strength = EffectLevelUtil.getLevelDouble(plugin, effect, level, "pull-strength",
                pullStrengthPerLevel * level);

        double defaultVerticalBoost = section != null ? section.getDouble("vertical-boost", 0.05D) : 0.05D;
        double verticalBoost = EffectLevelUtil.getLevelDouble(plugin, effect, level, "vertical-boost",
                defaultVerticalBoost);

        double defaultMaxVelocity = section != null ? section.getDouble("max-velocity", 1.2D) : 1.2D;
        double maxVelocity = EffectLevelUtil.getLevelDouble(plugin, effect, level, "max-velocity",
                defaultMaxVelocity);

        Location target = center.clone().add(0.5D, 1.0D, 0.5D);

        for (Entity entity : center.getWorld().getEntities()) {
            if (entity instanceof LivingEntity
                    && MobUtil.isHostile(entity)
                    && RangeUtil.isInsideHorizontalRange(entity.getLocation(), center, beacon.getRange())) {

                Vector direction = target.toVector().subtract(entity.getLocation().toVector());
                if (direction.lengthSquared() > 0.01D) {
                    direction.normalize().multiply(strength);
                    direction.setY(verticalBoost);

                    Vector velocity = entity.getVelocity().add(direction);
                    if (velocity.length() > maxVelocity) {
                        velocity.normalize().multiply(maxVelocity);
                    }

                    entity.setVelocity(velocity);
                }
            }
        }
    }
}