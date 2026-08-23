package cl.dynasty.nexusbeacon.platform.legacy;

public enum LegacyCapabilityStatus {
    AVAILABLE(true),
    DEFERRED(false),
    NOT_WIRED(false),
    OPTIONAL(false),
    UNAVAILABLE(false),
    DISABLED_ON_PLATFORM(false);

    private final boolean available;

    LegacyCapabilityStatus(boolean available) {
        this.available = available;
    }

    public boolean isAvailable() {
        return available;
    }
}
