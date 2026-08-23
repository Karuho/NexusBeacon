package cl.dynasty.nexusbeacon.platform.legacy;

final class LegacyRecipeValidationException extends IllegalArgumentException {
    private final LegacyRecipeRegistrationStatus status;

    LegacyRecipeValidationException(LegacyRecipeRegistrationStatus status, String message) {
        super(message);
        this.status = status;
    }

    LegacyRecipeRegistrationStatus getStatus() { return status; }
}
