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

public class BurnerExecutor implements EffectExecutor {

    private final DynaBeaconPlugin plugin;

    public BurnerExecutor(DynaBeaconPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getType() {
        return "BURNER";
    }

    @Override
    public void tick(BeaconData beacon, BeaconEffect effect) {
        Location center = beacon.getLocation();
        if (center == null || center.getWorld() == null) return;

        ConfigurationSection section = plugin.getConfigManager()
                .getEffectsConfig()
                .getConfigurationSection("effects." + effect.getId());

        int level = beacon.getEffectLevel(effect.getId());
        int fireTicks = section != null ? section.getInt("fire-ticks-per-level", 60) * Math.max(1, level) : 60;

        for (Entity entity : center.getWorld().getEntities()) {
            if (!(entity instanceof LivingEntity)) continue;
            if (!MobUtil.isHostile(entity)) continue;
            if (!RangeUtil.isInsideHorizontalRange(entity.getLocation(), center, beacon.getRange())) continue;

            entity.setFireTicks(fireTicks);
        }
    }
}