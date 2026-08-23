package cl.dynasty.nexusbeacon.platform.legacy;

public final class LegacyRecipeRegistrationResult {
    private final LegacyRecipeRegistrationStatus status;
    private final String diagnostic;

    private LegacyRecipeRegistrationResult(LegacyRecipeRegistrationStatus status, String diagnostic) {
        this.status = status;
        this.diagnostic = diagnostic;
    }

    public static LegacyRecipeRegistrationResult of(LegacyRecipeRegistrationStatus status, String diagnostic) {
        if (status == null) throw new NullPointerException("status");
        return new LegacyRecipeRegistrationResult(status, diagnostic == null ? "" : diagnostic);
    }

    public LegacyRecipeRegistrationStatus getStatus() { return status; }
    public String getDiagnostic() { return diagnostic; }
    public boolean isAvailable() { return status.isAvailable(); }
}
