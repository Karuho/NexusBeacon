package cl.dynasty.dynabeacon.api;

import org.bukkit.Material;
import org.bukkit.potion.PotionEffectType;

public interface VersionAdapter {

    Material material(String name);

    PotionEffectType potion(String name);

    String getServerVersion();
}