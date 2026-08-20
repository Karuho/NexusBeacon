package cl.dynasty.nexusbeacon.platform.legacy;

public enum LegacyGuiMutationResult {
    COMMITTED,
    UNAUTHORIZED,
    MISSING_BEACON,
    STALE_LOCATION,
    UNSUPPORTED,
    NOT_ACQUIRED,
    RUNTIME_UNAVAILABLE
}
