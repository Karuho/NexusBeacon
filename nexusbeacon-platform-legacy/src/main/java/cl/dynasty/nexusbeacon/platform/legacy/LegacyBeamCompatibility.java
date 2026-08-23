package cl.dynasty.nexusbeacon.platform.legacy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class LegacyBeamCompatibility {
    private final LegacyParticleService particles;

    public LegacyBeamCompatibility(LegacyParticleService particles) {
        if (particles == null) throw new NullPointerException("particles");
        this.particles = particles;
    }

    public LegacyBeamStyleCompatibility classify(LegacyBeamStylePlan style) {
        if (style == null) throw new NullPointerException("style");
        LegacyParticleResolution particle = particles.resolve(style.getParticleName());
        if (!particle.isSupported()) {
            return new LegacyBeamStyleCompatibility(style, particle,
                    LegacyBeamCompatibilityStatus.UNSUPPORTED, "particle unavailable");
        }
        if (particle.getCompatibility() == LegacyParticleCompatibility.VISUAL_APPROXIMATION) {
            return new LegacyBeamStyleCompatibility(style, particle,
                    LegacyBeamCompatibilityStatus.VISUAL_DEGRADATION,
                    "particle uses explicit visual-only approximation");
        }
        if (style.getColor() != null && !particle.isColorSupported()) {
            return new LegacyBeamStyleCompatibility(style, particle,
                    LegacyBeamCompatibilityStatus.VISUAL_DEGRADATION, "color unavailable");
        }
        if (style.getSize() != 1.0F && !particle.isSizeSupported()) {
            return new LegacyBeamStyleCompatibility(style, particle,
                    LegacyBeamCompatibilityStatus.VISUAL_DEGRADATION,
                    "Legacy particle size is fixed; RGB and geometry remain exact");
        }
        if (particle.getCompatibility() == LegacyParticleCompatibility.EXACT) {
            return new LegacyBeamStyleCompatibility(style, particle,
                    LegacyBeamCompatibilityStatus.FULL, "exact physical particle");
        }
        return new LegacyBeamStyleCompatibility(style, particle,
                LegacyBeamCompatibilityStatus.FULL_WITH_PARTICLE_ALIAS,
                "stable Legacy particle alias/effect equivalent");
    }

    public List<LegacyBeamStylePlan> selectable(List<LegacyBeamStylePlan> styles) {
        List<LegacyBeamStylePlan> result = new ArrayList<LegacyBeamStylePlan>();
        for (LegacyBeamStylePlan style : styles) {
            LegacyBeamStyleCompatibility classification = classify(style);
            if (classification.getStatus() != LegacyBeamCompatibilityStatus.UNSUPPORTED
                    && classification.getParticle().getCompatibility()
                            != LegacyParticleCompatibility.VISUAL_APPROXIMATION) {
                result.add(style);
            }
        }
        return Collections.unmodifiableList(result);
    }
}
