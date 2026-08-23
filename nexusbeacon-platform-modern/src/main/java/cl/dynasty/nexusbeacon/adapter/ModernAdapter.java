package cl.dynasty.nexusbeacon.adapter;

import java.util.function.Function;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffectType;

import cl.dynasty.nexusbeacon.api.VersionAdapter;
import cl.dynasty.nexusbeacon.platform.api.MaterialContext;
import cl.dynasty.nexusbeacon.platform.api.MaterialResolution;
import cl.dynasty.nexusbeacon.platform.api.MaterialResolutionStatus;
import cl.dynasty.nexusbeacon.platform.api.PotionEffectResolution;

public class ModernAdapter implements VersionAdapter {
    private final Function<String, PotionEffectType> potionLookup;

    public ModernAdapter() {
        this(PotionEffectType::getByName);
    }

    ModernAdapter(Function<String, PotionEffectType> potionLookup) {
        this.potionLookup = potionLookup;
    }

    @Override
    public Material material(String name) {
        return resolveMaterial(name, MaterialContext.REQUIRED_ITEM).getMaterial().orElse(null);
    }

    @Override
    public PotionEffectType potion(String name) {
        return resolvePotionEffect(name).getEffectType().orElse(null);
    }

    @Override
    public MaterialResolution resolveMaterial(String identifier, MaterialContext context) {
        if (context == null) throw new NullPointerException("context");
        if (identifier == null || identifier.trim().isEmpty()) {
            return failedMaterial(identifier, context, MaterialResolutionStatus.INVALID_IDENTIFIER);
        }

        String normalized = normalize(identifier);
        if (!normalized.matches("[A-Z0-9_]+")) {
            return failedMaterial(identifier, context, MaterialResolutionStatus.INVALID_IDENTIFIER);
        }

        Material resolved = Material.matchMaterial(normalized);
        if (resolved == null) {
            return failedMaterial(identifier, context, MaterialResolutionStatus.UNSUPPORTED);
        }
        return MaterialResolution.resolved(identifier, context, resolved);
    }

    @Override
    public PotionEffectResolution resolvePotionEffect(String identifier) {
        if (identifier == null || identifier.trim().isEmpty()) {
            return PotionEffectResolution.failed(identifier, PotionEffectResolution.Status.INVALID_IDENTIFIER);
        }
        String normalized = normalize(identifier);
        if (!normalized.matches("[A-Z0-9_]+")) {
            return PotionEffectResolution.failed(identifier, PotionEffectResolution.Status.INVALID_IDENTIFIER);
        }
        PotionEffectType resolved = potionLookup.apply(normalized);
        return resolved == null
                ? PotionEffectResolution.failed(identifier, PotionEffectResolution.Status.UNSUPPORTED)
                : PotionEffectResolution.resolved(identifier, resolved);
    }

    private MaterialResolution failedMaterial(String identifier, MaterialContext context,
            MaterialResolutionStatus status) {
        Material fallback = context.isVisualFallbackAllowed() ? Material.STONE : null;
        return MaterialResolution.failed(identifier, context, status, fallback);
    }

    private String normalize(String identifier) {
        return identifier.trim().toUpperCase()
                .replace("MINECRAFT:", "")
                .replace(" ", "_")
                .replace("-", "_");
    }

    @Override
    public String getServerVersion() {
        return Bukkit.getBukkitVersion();
    }
}
