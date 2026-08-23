package cl.dynasty.nexusbeacon.platform.legacy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class LegacyStorageLoadResult {
    private final LegacyStorageLoadStatus status;
    private final List<LegacyBeaconState> beacons;
    private final String diagnostic;

    private LegacyStorageLoadResult(LegacyStorageLoadStatus status, List<LegacyBeaconState> beacons,
            String diagnostic) {
        this.status = status;
        this.beacons = Collections.unmodifiableList(new ArrayList<LegacyBeaconState>(beacons));
        this.diagnostic = diagnostic;
    }

    public static LegacyStorageLoadResult success(List<LegacyBeaconState> beacons) {
        if (beacons == null) throw new NullPointerException("beacons");
        return new LegacyStorageLoadResult(beacons.isEmpty() ? LegacyStorageLoadStatus.EMPTY
                : LegacyStorageLoadStatus.READY, beacons, null);
    }

    public static LegacyStorageLoadResult failure(LegacyStorageLoadStatus status, String diagnostic) {
        if (status == null || status.isSuccessful()) throw new IllegalArgumentException("Failure status required");
        return new LegacyStorageLoadResult(status, Collections.<LegacyBeaconState>emptyList(), diagnostic);
    }

    public LegacyStorageLoadStatus getStatus() { return status; }
    public List<LegacyBeaconState> getBeacons() { return beacons; }
    public String getDiagnostic() { return diagnostic; }
    public boolean isSuccessful() { return status.isSuccessful(); }
}
