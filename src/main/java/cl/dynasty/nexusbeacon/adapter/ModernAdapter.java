package cl.dynasty.nexusbeacon.adapter;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffectType;

import cl.dynasty.nexusbeacon.api.VersionAdapter;

public class ModernAdapter implements VersionAdapter {

    @Override
    public Material material(String name) {
        if (name == null || name.trim().isEmpty()) {
            return Material.STONE;
        }

        String normalized = name.toUpperCase()
                .replace("MINECRAFT:", "")
                .replace(" ", "_")
                .replace("-", "_");

        Material material = Material.matchMaterial(normalized);

        return material != null ? material : Material.STONE;
    }

    @Override
    public PotionEffectType potion(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }

        String normalized = name.toUpperCase()
                .replace("MINECRAFT:", "")
                .replace(" ", "_")
                .replace("-", "_");

        return PotionEffectType.getByName(normalized);
    }

    @Override
    public String getServerVersion() {
        return Bukkit.getBukkitVersion();
    }
}