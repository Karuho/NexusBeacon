package cl.dynasty.nexusbeacon.platform.legacy;

public final class LegacyBeamRenderResult {
    private final LegacyBeamStyleCompatibility compatibility;
    private final int points;
    private final boolean scheduled;

    LegacyBeamRenderResult(LegacyBeamStyleCompatibility compatibility, int points, boolean scheduled) {
        this.compatibility = compatibility;
        this.points = points;
        this.scheduled = scheduled;
    }

    public LegacyBeamStyleCompatibility getCompatibility() { return compatibility; }
    public int getPoints() { return points; }
    public boolean isScheduled() { return scheduled; }
}
