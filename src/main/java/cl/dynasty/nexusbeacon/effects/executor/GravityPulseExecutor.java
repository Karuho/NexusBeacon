package cl.dynasty.nexusbeacon.effects.executor;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import cl.dynasty.nexusbeacon.NexusBeaconPlugin;
import cl.dynasty.nexusbeacon.effects.BeaconEffect;
import cl.dynasty.nexusbeacon.effects.EffectLevelUtil;
import cl.dynasty.nexusbeacon.model.BeaconData;
import cl.dynasty.nexusbeacon.util.DebugLogger;
import cl.dynasty.nexusbeacon.util.MobUtil;
import cl.dynasty.nexusbeacon.util.RangeUtil;

public class GravityPulseExecutor implements EffectExecutor {

    private final NexusBeaconPlugin plugin;

    public GravityPulseExecutor(NexusBeaconPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getType() {
        return "GRAVITY_PULSE";
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

        DebugLogger.log(plugin, effect.getType() + ":" + beacon.getId(),
                "EffectExecutor type=" + effect.getType()
                        + " effect=" + effect.getId()
                        + " level=" + level
                        + " strength=" + strength
                        + " verticalBoost=" + verticalBoost
                        + " maxVelocity=" + maxVelocity
                        + " range=" + beacon.getRange());

        Location target = center.clone().add(0.5D, 1.0D, 0.5D);

        int range = beacon.getRange();
        double searchRange = range;

        for (Entity entity : center.getWorld().getNearbyEntities(
                center,
                searchRange,
                searchRange,
                searchRange)) {
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