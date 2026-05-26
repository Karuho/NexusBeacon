package cl.dynasty.nexusbeacon.effects;

import java.util.List;

import org.bukkit.Material;

import cl.dynasty.nexusbeacon.model.BeaconData;

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