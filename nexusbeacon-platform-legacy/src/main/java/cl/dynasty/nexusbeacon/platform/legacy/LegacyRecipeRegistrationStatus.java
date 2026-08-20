package cl.dynasty.nexusbeacon.platform.legacy;

public enum LegacyRecipeRegistrationStatus {
    REGISTERED,
    ALREADY_PRESENT,
    INVALID_CONFIG,
    UNSUPPORTED_INGREDIENT,
    BUKKIT_REJECTED,
    DISABLED;

    public boolean isAvailable() {
        return this == REGISTERED || this == ALREADY_PRESENT;
    }
}
