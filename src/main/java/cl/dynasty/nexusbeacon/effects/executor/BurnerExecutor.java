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

public class BurnerExecutor implements EffectExecutor {

    private final NexusBeaconPlugin plugin;

    public BurnerExecutor(NexusBeaconPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getType() {
        return "BURNER";
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
        int fireTicks = EffectLevelUtil.getLevelInt(plugin, effect, level, "fire-ticks",
                section.getInt("fire-ticks-per-level", 60) * level);

        for (Entity entity : center.getWorld().getEntities()) {
            if (!(entity instanceof LivingEntity))
                continue;
            if (!MobUtil.isHostile(entity))
                continue;
            if (!RangeUtil.isInsideHorizontalRange(entity.getLocation(), center, beacon.getRange()))
                continue;

            entity.setFireTicks(fireTicks);
        }
    }
}