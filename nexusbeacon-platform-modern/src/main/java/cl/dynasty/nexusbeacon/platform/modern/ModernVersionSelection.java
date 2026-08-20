package cl.dynasty.nexusbeacon.platform.modern;

import java.util.Objects;
import java.util.Optional;

import cl.dynasty.nexusbeacon.platform.MinecraftVersion;

public final class ModernVersionSelection {
    private final MinecraftVersion minecraftVersion;
    private final int javaFeatureVersion;
    private final ModernSelectionStatus status;
    private final ModernPlatformProfile profile;

    private ModernVersionSelection(MinecraftVersion minecraftVersion, int javaFeatureVersion,
            ModernSelectionStatus status, ModernPlatformProfile profile) {
        this.minecraftVersion = Objects.requireNonNull(minecraftVersion, "minecraftVersion");
        this.javaFeatureVersion = javaFeatureVersion;
        this.status = Objects.requireNonNull(status, "status");
        this.profile = profile;
    }

    static ModernVersionSelection supported(MinecraftVersion minecraft, int javaFeature,
            ModernPlatformProfile profile) {
        return new ModernVersionSelection(minecraft, javaFeature, ModernSelectionStatus.SUPPORTED, profile);
    }

    static ModernVersionSelection unsupportedJava(MinecraftVersion minecraft, int javaFeature,
            ModernPlatformProfile profile) {
        return new ModernVersionSelection(minecraft, javaFeature, ModernSelectionStatus.UNSUPPORTED_JAVA, profile);
    }

    static ModernVersionSelection unsupportedMinecraft(MinecraftVersion minecraft, int javaFeature) {
        return new ModernVersionSelection(minecraft, javaFeature, ModernSelectionStatus.UNSUPPORTED_MINECRAFT, null);
    }

    public MinecraftVersion getMinecraftVersion() { return minecraftVersion; }
    public int getJavaFeatureVersion() { return javaFeatureVersion; }
    public ModernSelectionStatus getStatus() { return status; }
    public Optional<ModernPlatformProfile> getProfile() { return Optional.ofNullable(profile); }
    public boolean isSupported() { return status == ModernSelectionStatus.SUPPORTED; }
}
