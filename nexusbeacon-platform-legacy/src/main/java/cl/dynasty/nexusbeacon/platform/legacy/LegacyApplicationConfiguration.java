package cl.dynasty.nexusbeacon.platform.legacy;

public final class LegacyApplicationConfiguration {
    private final String language;
    private final String storageType;
    private final String rangeMode;
    private final int configuredEffects;
    private final int potionEffects;
    private final int deferredEffects;
    private final int unsupportedEffects;
    private final int guiItems;
    private final int guiVisualFallbacks;
    private final int beamStyles;
    private final boolean recipeConfigured;

    LegacyApplicationConfiguration(String language, String storageType, String rangeMode,
            int configuredEffects, int potionEffects, int deferredEffects, int unsupportedEffects, int guiItems,
            int guiVisualFallbacks, int beamStyles, boolean recipeConfigured) {
        this.language = language;
        this.storageType = storageType;
        this.rangeMode = rangeMode;
        this.configuredEffects = configuredEffects;
        this.potionEffects = potionEffects;
        this.deferredEffects = deferredEffects;
        this.unsupportedEffects = unsupportedEffects;
        this.guiItems = guiItems;
        this.guiVisualFallbacks = guiVisualFallbacks;
        this.beamStyles = beamStyles;
        this.recipeConfigured = recipeConfigured;
    }

    public String getLanguage() { return language; }
    public String getStorageType() { return storageType; }
    public String getRangeMode() { return rangeMode; }
    public int getConfiguredEffects() { return configuredEffects; }
    public int getPotionEffects() { return potionEffects; }
    public int getDeferredEffects() { return deferredEffects; }
    public int getUnsupportedEffects() { return unsupportedEffects; }
    public int getGuiItems() { return guiItems; }
    public int getGuiVisualFallbacks() { return guiVisualFallbacks; }
    public int getBeamStyles() { return beamStyles; }
    public boolean isRecipeConfigured() { return recipeConfigured; }
}
