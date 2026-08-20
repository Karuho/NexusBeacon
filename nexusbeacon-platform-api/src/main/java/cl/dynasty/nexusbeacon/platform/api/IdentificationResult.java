package cl.dynasty.nexusbeacon.platform.api;

import java.util.Optional;

public final class IdentificationResult {

    public enum Status {
        RECOGNIZED,
        NOT_RECOGNIZED,
        MALFORMED_METADATA
    }

    public enum Evidence {
        PERSISTENT_MARKER,
        LEGACY_DISPLAY_NAME,
        NONE
    }

    private final Status status;
    private final ItemIdentity identity;
    private final Evidence evidence;

    private IdentificationResult(Status status, ItemIdentity identity, Evidence evidence) {
        this.status = status;
        this.identity = identity;
        this.evidence = evidence;
    }

    public static IdentificationResult recognized(ItemIdentity identity, Evidence evidence) {
        return new IdentificationResult(Status.RECOGNIZED, identity, evidence);
    }

    public static IdentificationResult notRecognized() {
        return new IdentificationResult(Status.NOT_RECOGNIZED, null, Evidence.NONE);
    }

    public static IdentificationResult malformed() {
        return new IdentificationResult(Status.MALFORMED_METADATA, null, Evidence.PERSISTENT_MARKER);
    }

    public Status getStatus() {
        return status;
    }

    public Optional<ItemIdentity> getIdentity() {
        return Optional.ofNullable(identity);
    }

    public Evidence getEvidence() {
        return evidence;
    }

    public boolean isRecognized() {
        return status == Status.RECOGNIZED;
    }
}
