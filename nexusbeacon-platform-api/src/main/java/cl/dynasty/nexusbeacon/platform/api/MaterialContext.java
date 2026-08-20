package cl.dynasty.nexusbeacon.platform.api;

public enum MaterialContext {
    GUI_ICON(true),
    EFFECT_ICON(false),
    RECIPE_INGREDIENT(false),
    PAYMENT(false),
    REQUIRED_ITEM(false),
    BLOCK_MATCH(false);

    private final boolean visualFallbackAllowed;

    MaterialContext(boolean visualFallbackAllowed) {
        this.visualFallbackAllowed = visualFallbackAllowed;
    }

    public boolean isVisualFallbackAllowed() { return visualFallbackAllowed; }
}
