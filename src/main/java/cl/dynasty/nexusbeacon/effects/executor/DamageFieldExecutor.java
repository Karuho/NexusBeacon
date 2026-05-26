package cl.dynasty.nexusbeacon.effects.executor;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import cl.dynasty.nexusbeacon.NexusBeaconPlugin;
import cl.dynasty.nexusbeacon.effects.BeaconEffect;
import cl.dynasty.nexusbeacon.effects.EffectLevelUtil;
import cl.dynasty.nexusbeacon.model.BeaconData;
import cl.dynasty.nexusbeacon.util.MobUtil;
import cl.dynasty.nexusbeacon.util.RangeUtil;

public class DamageFieldExecutor implements EffectExecutor {

    private final NexusBeaconPlugin plugin;

    public DamageFieldExecutor(NexusBeaconPlugin plugin) {
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

        int level = Math.max(1, beacon.getEffectLevel(effect.getId()));
        double damage = EffectLevelUtil.getLevelDouble(plugin, effect, level, "damage",
        section.getDouble("damage-per-level", 1.0D) * level);

        for (Entity entity : center.getWorld().getEntities()) {
            if (!(entity instanceof LivingEntity)) continue;
            if (!MobUtil.isHostile(entity)) continue;
            if (!RangeUtil.isInsideHorizontalRange(entity.getLocation(), center, beacon.getRange())) continue;

            ((LivingEntity) entity).damage(damage);
        }
    }
}