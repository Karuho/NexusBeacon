package cl.dynasty.nexusbeacon.api;

import org.bukkit.Material;
import org.bukkit.potion.PotionEffectType;

import cl.dynasty.nexusbeacon.platform.api.MaterialResolver;
import cl.dynasty.nexusbeacon.platform.api.PotionEffectResolver;

public interface VersionAdapter extends MaterialResolver, PotionEffectResolver {

    Material material(String name);

    PotionEffectType potion(String name);

    String getServerVersion();
}
