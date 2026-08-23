package cl.dynasty.nexusbeacon.platform.legacy;

import org.bukkit.Effect;

public final class LegacyParticleResolution {
    private final String semanticName;
    private final String physicalName;
    private final LegacyParticleCompatibility compatibility;
    private final Effect effect;
    private final boolean colorSupported;
    private final boolean sizeSupported;

    LegacyParticleResolution(String semanticName, String physicalName,
            LegacyParticleCompatibility compatibility, Effect effect,
            boolean colorSupported, boolean sizeSupported) {
        this.semanticName = semanticName;
        this.physicalName = physicalName;
        this.compatibility = compatibility;
        this.effect = effect;
        this.colorSupported = colorSupported;
        this.sizeSupported = sizeSupported;
    }

    public String getSemanticName() { return semanticName; }
    public String getPhysicalName() { return physicalName; }
    public LegacyParticleCompatibility getCompatibility() { return compatibility; }
    public Effect getEffect() { return effect; }
    public boolean isColorSupported() { return colorSupported; }
    public boolean isSizeSupported() { return sizeSupported; }
    public boolean isSupported() {
        return compatibility != LegacyParticleCompatibility.INVALID
                && compatibility != LegacyParticleCompatibility.UNSUPPORTED;
    }
    public boolean isVisuallyDegraded() {
        return compatibility == LegacyParticleCompatibility.VISUAL_APPROXIMATION || !sizeSupported;
    }
}
