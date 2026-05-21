package cl.dynasty.dynabeacon.effects;

import java.util.List;

import org.bukkit.Material;

import cl.dynasty.dynabeacon.model.BeaconData;

public interface BeaconEffect {

    String getId();

    String getType();

    String getDisplayName();

    List<String> getDescription();

    Material getIcon();

    int getMaxLevel();

    int getPowerConsumption();

    void tick(BeaconData beacon);
}