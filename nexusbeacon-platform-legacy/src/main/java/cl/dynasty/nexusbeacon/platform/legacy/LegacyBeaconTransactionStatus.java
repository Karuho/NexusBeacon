package cl.dynasty.nexusbeacon.platform.legacy;

public enum LegacyBeaconTransactionStatus {
    COMMITTED,
    NOT_RECOGNIZED,
    MALFORMED_ITEM,
    DUPLICATE_LOCATION_OR_ID,
    UNMANAGED_BEACON,
    STATE_WORLD_MISMATCH,
    STORAGE_FAILURE,
    WORLD_MUTATION_FAILURE
}
