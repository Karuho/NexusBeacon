package cl.dynasty.nexusbeacon.platform.classic;

import java.util.Locale;
import org.bukkit.Particle;

public final class ClassicParticleResolver {
    public ClassicParticleResolution resolve(String identifier) {
        if (identifier == null || identifier.trim().isEmpty()) return ClassicParticleResolution.failed(ClassicParticleResolution.Status.INVALID_IDENTIFIER);
        try { return ClassicParticleResolution.resolved(Particle.valueOf(identifier.trim().toUpperCase(Locale.ROOT))); }
        catch (IllegalArgumentException missing) { return ClassicParticleResolution.failed(ClassicParticleResolution.Status.UNSUPPORTED); }
    }
}
