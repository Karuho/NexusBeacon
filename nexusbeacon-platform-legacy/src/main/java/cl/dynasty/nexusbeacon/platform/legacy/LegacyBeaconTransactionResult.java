package cl.dynasty.nexusbeacon.platform.legacy;

public final class LegacyBeaconTransactionResult {
    private final LegacyBeaconTransactionStatus status;
    private final LegacyBeaconState beacon;
    private final String diagnostic;

    private LegacyBeaconTransactionResult(LegacyBeaconTransactionStatus status,
            LegacyBeaconState beacon, String diagnostic) {
        this.status = status;
        this.beacon = beacon;
        this.diagnostic = diagnostic;
    }

    public static LegacyBeaconTransactionResult of(LegacyBeaconTransactionStatus status) {
        return new LegacyBeaconTransactionResult(status, null, null);
    }

    public static LegacyBeaconTransactionResult committed(LegacyBeaconState beacon) {
        return new LegacyBeaconTransactionResult(LegacyBeaconTransactionStatus.COMMITTED, beacon, null);
    }

    public static LegacyBeaconTransactionResult failure(LegacyBeaconTransactionStatus status, String diagnostic) {
        return new LegacyBeaconTransactionResult(status, null, diagnostic);
    }

    public LegacyBeaconTransactionStatus getStatus() { return status; }
    public LegacyBeaconState getBeacon() { return beacon; }
    public String getDiagnostic() { return diagnostic; }
    public boolean isCommitted() { return status == LegacyBeaconTransactionStatus.COMMITTED; }
}
