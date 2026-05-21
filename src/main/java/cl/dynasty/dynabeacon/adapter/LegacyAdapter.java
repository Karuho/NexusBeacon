package cl.dynasty.dynabeacon.adapter;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffectType;

import cl.dynasty.dynabeacon.api.VersionAdapter;

public class LegacyAdapter implements VersionAdapter {

    @Override
    public Material material(String modernName, String legacyName) {
        Material material = safeMaterial(legacyName);

        if (material == null) {
            material = safeMaterial(modernName);
        }

        return material != null ? material : Material.STONE;
    }

    private Material safeMaterial(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }

        try {
            return Material.matchMaterial(name.toUpperCase());
        } catch (Exception ignored) {
        }

        try {
            return Material.valueOf(name.toUpperCase());
        } catch (Exception ignored) {
        }

        return null;
    }

    @Override
    public PotionEffectType potion(String modernName, String legacyName) {
        PotionEffectType type = PotionEffectType.getByName(legacyName);

        if (type == null) {
            type = PotionEffectType.getByName(modernName);
        }

        return type;
    }

    @Override
    public boolean isModern() {
        return false;
    }

    @Override
    public String getServerVersion() {
        return Bukkit.getBukkitVersion();
    }
}