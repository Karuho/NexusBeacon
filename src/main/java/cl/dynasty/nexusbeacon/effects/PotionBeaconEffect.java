package cl.dynasty.nexusbeacon.effects;

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

import cl.dynasty.nexusbeacon.model.BeaconData;
import cl.dynasty.nexusbeacon.util.RangeUtil;

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

        Location center = beacon.getLocation();
        int range = beacon.getRange();
        double searchRange = range;

        for (Entity entity : center.getWorld().getNearbyEntities(
                center,
                searchRange,
                searchRange,
                searchRange)) {

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

            living.addPotionEffect(createPotionEffect(level), true);
        }
    }

    PotionEffect createPotionEffect(int level) {
        return new PotionEffect(potionType, durationTicks, calculateAmplifier(level), isAmbient(), hasParticles());
    }

    int calculateAmplifier(int level) { return Math.max(0, (level * amplifierPerLevel) - 1); }
    int getDurationTicks() { return durationTicks; }
    boolean isAmbient() { return true; }
    boolean hasParticles() { return true; }

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
