package cl.dynasty.nexusbeacon.platform.modern;

import java.util.Objects;

public final class ModernPlatformProfile {
    private final ModernVersionFamily family;
    private final int minimumJavaFeature;

    public ModernPlatformProfile(ModernVersionFamily family, int minimumJavaFeature) {
        this.family = Objects.requireNonNull(family, "family");
        if (minimumJavaFeature <= 0) throw new IllegalArgumentException("minimumJavaFeature must be positive");
        this.minimumJavaFeature = minimumJavaFeature;
    }

    public ModernVersionFamily getFamily() { return family; }
    public int getMinimumJavaFeature() { return minimumJavaFeature; }
}
