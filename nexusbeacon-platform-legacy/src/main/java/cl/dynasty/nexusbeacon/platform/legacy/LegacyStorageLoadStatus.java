package cl.dynasty.nexusbeacon.platform.legacy;

public enum LegacyStorageLoadStatus {
    READY(true),
    EMPTY(true),
    CORRUPT(false),
    UNAVAILABLE(false);

    private final boolean successful;

    LegacyStorageLoadStatus(boolean successful) { this.successful = successful; }
    public boolean isSuccessful() { return successful; }
}
