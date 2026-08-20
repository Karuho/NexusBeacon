package cl.dynasty.nexusbeacon.platform.legacy;

/** World mutation seam used to compensate a durable removal if block removal fails. */
public interface LegacyWorldBeaconMutation {
    boolean isBeacon();
    void removeBeacon();
}
