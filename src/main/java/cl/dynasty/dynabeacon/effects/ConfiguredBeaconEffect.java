package cl.dynasty.dynabeacon.effects;

import java.util.List;

import org.bukkit.Material;

import cl.dynasty.dynabeacon.model.BeaconData;

public class ConfiguredBeaconEffect implements BeaconEffect {

    private final String id;
    private final String type;
    private final String displayName;
    private final List<String> description;
    private final Material icon;
    private final int maxLevel;
    private final int powerConsumption;

    public ConfiguredBeaconEffect(String id, String type, String displayName, List<String> description,
                                  Material icon, int maxLevel, int powerConsumption) {
        this.id = id;
        this.type = type;
        this.displayName = displayName;
        this.description = description;
        this.icon = icon;
        this.maxLevel = maxLevel;
        this.powerConsumption = powerConsumption;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getType() {
        return type;
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
        // Lo ejecuta EffectExecutorRegistry según getType().
    }
}