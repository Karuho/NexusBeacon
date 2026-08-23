package cl.dynasty.nexusbeacon.platform.api;

import java.util.Objects;
import java.util.Optional;

import org.bukkit.Material;

public final class MaterialResolution {
    private final String identifier;
    private final MaterialContext context;
    private final MaterialResolutionStatus status;
    private final Material material;
    private final boolean fallbackUsed;

    private MaterialResolution(String identifier, MaterialContext context, MaterialResolutionStatus status,
            Material material, boolean fallbackUsed) {
        this.identifier = identifier;
        this.context = Objects.requireNonNull(context, "context");
        this.status = Objects.requireNonNull(status, "status");
        this.material = material;
        this.fallbackUsed = fallbackUsed;
    }

    public static MaterialResolution resolved(String identifier, MaterialContext context, Material material) {
        return new MaterialResolution(identifier, context, MaterialResolutionStatus.RESOLVED,
                Objects.requireNonNull(material, "material"), false);
    }

    public static MaterialResolution failed(String identifier, MaterialContext context,
            MaterialResolutionStatus status, Material visualFallback) {
        if (status == MaterialResolutionStatus.RESOLVED) throw new IllegalArgumentException("failure status required");
        return new MaterialResolution(identifier, context, status, visualFallback, visualFallback != null);
    }

    public String getIdentifier() { return identifier; }
    public MaterialContext getContext() { return context; }
    public MaterialResolutionStatus getStatus() { return status; }
    public Optional<Material> getMaterial() { return Optional.ofNullable(material); }
    public boolean isResolved() { return status == MaterialResolutionStatus.RESOLVED; }
    public boolean isFallbackUsed() { return fallbackUsed; }
}
