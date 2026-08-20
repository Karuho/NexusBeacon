package cl.dynasty.nexusbeacon.platform.api;

import java.util.Optional;

import org.bukkit.potion.PotionEffectType;

public final class PotionEffectResolution {
    public enum Status { RESOLVED, INVALID_IDENTIFIER, UNSUPPORTED }

    private final String identifier;
    private final Status status;
    private final PotionEffectType effectType;

    private PotionEffectResolution(String identifier, Status status, PotionEffectType effectType) {
        this.identifier = identifier;
        this.status = status;
        this.effectType = effectType;
    }

    public static PotionEffectResolution resolved(String identifier, PotionEffectType effectType) {
        if (effectType == null) throw new NullPointerException("effectType");
        return new PotionEffectResolution(identifier, Status.RESOLVED, effectType);
    }

    public static PotionEffectResolution failed(String identifier, Status status) {
        if (status == Status.RESOLVED) throw new IllegalArgumentException("failure status required");
        return new PotionEffectResolution(identifier, status, null);
    }

    public String getIdentifier() { return identifier; }
    public Status getStatus() { return status; }
    public Optional<PotionEffectType> getEffectType() { return Optional.ofNullable(effectType); }
    public boolean isResolved() { return status == Status.RESOLVED; }
}
