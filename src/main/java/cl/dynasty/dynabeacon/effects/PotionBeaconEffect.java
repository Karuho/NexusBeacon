package cl.dynasty.dynabeacon.effects;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import cl.dynasty.dynabeacon.model.BeaconData;
import cl.dynasty.dynabeacon.util.RangeUtil;

public class PotionBeaconEffect implements BeaconEffect {

    private final String id;
    private final String displayName;
    private final List<String> description;
    private final Material icon;
    private final PotionEffectType potionType;
    private final String target;
    private final int amplifierPerLevel;
    private final int durationTicks;
    private final int maxLevel;
    private final int powerConsumption;

    public PotionBeaconEffect(
            String id,
            String displayName,
            List<String> description,
            Material icon,
            PotionEffectType potionType,
            String target,
            int amplifierPerLevel,
            int durationTicks,
            int maxLevel,
            int powerConsumption) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.icon = icon;
        this.potionType = potionType;
        this.target = target != null ? target : "PLAYERS";
        this.amplifierPerLevel = amplifierPerLevel;
        this.durationTicks = durationTicks;
        this.maxLevel = maxLevel;
        this.powerConsumption = powerConsumption;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getType() {
        return "POTION";
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public List<String> getDescription() {
        return description;
    }

    @Override
    public Material getIcon() {
        return icon;
    }

    @Override
    public int getMaxLevel() {
        return maxLevel;
    }

    @Override
    public int getPowerConsumption() {
        return powerConsumption;
    }

    @Override
    public void tick(BeaconData beacon) {
        if (beacon == null || beacon.getLocation() == null || beacon.getLocation().getWorld() == null) {
            return;
        }

        int level = beacon.getEffectLevel(id);

        if (level <= 0) {
            return;
        }

        int amplifier = Math.max(0, (level * amplifierPerLevel) - 1);
        Location center = beacon.getLocation();
        int range = beacon.getRange();

        for (Entity entity : center.getWorld().getEntities()) {
            if (!(entity instanceof LivingEntity)) {
                continue;
            }

            if (!RangeUtil.isInsideHorizontalRange(entity.getLocation(), center, range)) {
                continue;
            }

            LivingEntity living = (LivingEntity) entity;

            if (!matchesTarget(living)) {
                continue;
            }

            living.addPotionEffect(new PotionEffect(potionType, durationTicks, amplifier, true, true), true);
        }
    }

    private boolean matchesTarget(LivingEntity entity) {
        if (target.equalsIgnoreCase("ALL_ENTITIES")) {
            return true;
        }

        if (target.equalsIgnoreCase("PLAYERS")) {
            return entity instanceof Player;
        }

        if (target.equalsIgnoreCase("MONSTERS")) {
            return entity instanceof Monster;
        }

        if (target.equalsIgnoreCase("ANIMALS")) {
            return entity instanceof Animals;
        }

        return entity instanceof Player;
    }
}