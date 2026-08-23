package cl.dynasty.nexusbeacon.platform.legacy;

public enum LegacyApplicationStateStatus {
    NEW,
    READY_EMPTY,
    READY,
    FAILED,
    CLOSED;

    public boolean isReady() { return this == READY_EMPTY || this == READY; }
}
