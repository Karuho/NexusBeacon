package cl.dynasty.dynabeacon.adapter;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffectType;

import cl.dynasty.dynabeacon.api.VersionAdapter;

public class ModernAdapter implements VersionAdapter {

    @Override
    public Material material(String modernName, String legacyName) {
        Material material = safeMaterial(modernName);

        if (material == null) {
            material = safeMaterial(legacyName);
        }

        return material != null ? material : Material.STONE;
    }

    private Material safeMaterial(String name) {
        if (name == null || name.trim().isEmpty())
            return null;

        String normalized = name.toUpperCase()
                .replace("MINECRAFT:", "")
                .replace(" ", "_")
                .replace("-", "_");

        if (normalized.equals("WEB"))
            normalized = "COBWEB";
        if (normalized.equals("TURTLE_SHELL"))
            normalized = "TURTLE_HELMET";

        try {
            java.lang.reflect.Method method = Material.class.getMethod("matchMaterial", String.class, boolean.class);
            Object result = method.invoke(null, normalized, false);
            if (result instanceof Material)
                return (Material) result;
        } catch (Exception ignored) {
        }

        try {
            Material material = Material.matchMaterial(normalized);
            if (material != null)
                return material;
        } catch (Exception ignored) {
        }

        try {
            return Material.valueOf(normalized);
        } catch (Exception ignored) {
        }

        return null;
    }

    @Override
    public PotionEffectType potion(String modernName, String legacyName) {
        PotionEffectType type = safePotion(modernName);

        if (type == null) {
            type = safePotion(legacyName);
        }

        return type;
    }

    private PotionEffectType safePotion(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }

        try {
            return PotionEffectType.getByName(name.toUpperCase());
        } catch (Exception exception) {
            return null;
        }
    }

    @Override
    public boolean isModern() {
        return true;
    }

    @Override
    public String getServerVersion() {
        return Bukkit.getBukkitVersion();
    }
}