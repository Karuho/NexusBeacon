package cl.dynasty.dynabeacon.api;

import org.bukkit.Material;
import org.bukkit.potion.PotionEffectType;

public interface VersionAdapter {

    Material material(String modernName, String legacyName);

    PotionEffectType potion(String modernName, String legacyName);

    boolean isModern();

    String getServerVersion();
}