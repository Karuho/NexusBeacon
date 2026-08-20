package cl.dynasty.nexusbeacon.platform.legacy;

public final class LegacyBeamStyleCompatibility {
    private final LegacyBeamStylePlan style;
    private final LegacyParticleResolution particle;
    private final LegacyBeamCompatibilityStatus status;
    private final String detail;

    LegacyBeamStyleCompatibility(LegacyBeamStylePlan style, LegacyParticleResolution particle,
            LegacyBeamCompatibilityStatus status, String detail) {
        this.style = style;
        this.particle = particle;
        this.status = status;
        this.detail = detail;
    }

    public LegacyBeamStylePlan getStyle() { return style; }
    public LegacyParticleResolution getParticle() { return particle; }
    public LegacyBeamCompatibilityStatus getStatus() { return status; }
    public String getDetail() { return detail; }
}
