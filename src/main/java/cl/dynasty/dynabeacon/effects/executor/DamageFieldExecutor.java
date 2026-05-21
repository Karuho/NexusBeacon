package cl.dynasty.dynabeacon.effects.executor;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import cl.dynasty.dynabeacon.DynaBeaconPlugin;
import cl.dynasty.dynabeacon.effects.BeaconEffect;
import cl.dynasty.dynabeacon.model.BeaconData;
import cl.dynasty.dynabeacon.util.MobUtil;
import cl.dynasty.dynabeacon.util.RangeUtil;

public class DamageFieldExecutor implements EffectExecutor {

    private final DynaBeaconPlugin plugin;

    public DamageFieldExecutor(DynaBeaconPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getType() {
        return "DAMAGE_FIELD";
    }

    @Override
    public void tick(BeaconData beacon, BeaconEffect effect) {
        Location center = beacon.getLocation();
        if (center == null || center.getWorld() == null) return;

        ConfigurationSection section = plugin.getConfigManager()
                .getEffectsConfig()
                .getConfigurationSection("effects." + effect.getId());

        int level = beacon.getEffectLevel(effect.getId());
        double damage = section != null ? section.getDouble("damage-per-level", 1.0D) * Math.max(1, level) : 1.0D;

        for (Entity entity : center.getWorld().getEntities()) {
            if (!(entity instanceof LivingEntity)) continue;
            if (!MobUtil.isHostile(entity)) continue;
            if (!RangeUtil.isInsideHorizontalRange(entity.getLocation(), center, beacon.getRange())) continue;

            ((LivingEntity) entity).damage(damage);
        }
    }
}