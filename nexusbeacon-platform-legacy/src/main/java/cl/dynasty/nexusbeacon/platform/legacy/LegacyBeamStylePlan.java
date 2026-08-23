package cl.dynasty.nexusbeacon.platform.legacy;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class LegacyBeamStylePlan {
    public static final String DEFAULT_STYLE_ID = "aqua";
    private final String id;
    private final String particleName;
    private final LegacyParticleColor color;
    private final float size;

    public LegacyBeamStylePlan(String id, String particleName, LegacyParticleColor color, float size) {
        if (id == null || id.trim().isEmpty()) throw new IllegalArgumentException("id is required");
        if (particleName == null || particleName.trim().isEmpty()) {
            throw new IllegalArgumentException("particleName is required");
        }
        if (size <= 0.0F) throw new IllegalArgumentException("size must be positive");
        this.id = id;
        this.particleName = particleName;
        this.color = color;
        this.size = size;
    }

    public String getId() { return id; }
    public String getParticleName() { return particleName; }
    public LegacyParticleColor getColor() { return color; }
    public float getSize() { return size; }

    public static List<LegacyBeamStylePlan> currentDefaults() {
        return Collections.unmodifiableList(Arrays.asList(
                new LegacyBeamStylePlan("aqua", "DUST", new LegacyParticleColor(0, 255, 255), 1.2F),
                new LegacyBeamStylePlan("red", "DUST", new LegacyParticleColor(255, 0, 0), 1.2F),
                new LegacyBeamStylePlan("green", "DUST", new LegacyParticleColor(0, 255, 0), 1.2F),
                new LegacyBeamStylePlan("purple", "DUST", new LegacyParticleColor(255, 0, 255), 1.2F),
                new LegacyBeamStylePlan("end_rod", "END_ROD", null, 1.0F)));
    }

    public static LegacyBeamStylePlan defaultStyle() {
        for (LegacyBeamStylePlan style : currentDefaults()) {
            if (DEFAULT_STYLE_ID.equals(style.getId())) return style;
        }
        throw new IllegalStateException("Default Legacy beam style is not configured");
    }
}
