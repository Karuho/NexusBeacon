package cl.dynasty.nexusbeacon.platform.modern;

import cl.dynasty.nexusbeacon.platform.MinecraftVersion;

public final class ModernVersionSelector {
    private static final ModernPlatformProfile PROFILE_1_13 =
            new ModernPlatformProfile(ModernVersionFamily.MODERN_1_13, 8);
    private static final ModernPlatformProfile PROFILE_1_21 =
            new ModernPlatformProfile(ModernVersionFamily.MODERN_1_21, 21);
    private static final ModernPlatformProfile PROFILE_26_2 =
            new ModernPlatformProfile(ModernVersionFamily.MODERN_26_2, 25);

    private ModernVersionSelector() {}

    public static ModernVersionSelection select(MinecraftVersion minecraft, int javaFeature) {
        if (minecraft == null) throw new NullPointerException("minecraft");
        if (javaFeature <= 0) throw new IllegalArgumentException("javaFeature must be positive");

        ModernPlatformProfile profile = profileFor(minecraft);
        if (profile == null) return ModernVersionSelection.unsupportedMinecraft(minecraft, javaFeature);
        if (javaFeature < profile.getMinimumJavaFeature()) {
            return ModernVersionSelection.unsupportedJava(minecraft, javaFeature, profile);
        }
        return ModernVersionSelection.supported(minecraft, javaFeature, profile);
    }

    private static ModernPlatformProfile profileFor(MinecraftVersion minecraft) {
        if (minecraft.getMajor() == 1 && minecraft.getMinor() == 13) return PROFILE_1_13;
        if (minecraft.getMajor() == 1 && minecraft.getMinor() == 21) return PROFILE_1_21;
        if (minecraft.getMajor() == 26 && minecraft.getMinor() == 2) return PROFILE_26_2;
        return null;
    }
}
