package cl.dynasty.nexusbeacon.platform.classic;

import java.util.Optional;
import org.bukkit.Particle;

public final class ClassicParticleResolution {
    public enum Status { RESOLVED, INVALID_IDENTIFIER, UNSUPPORTED }
    private final Status status; private final Particle particle;
    private ClassicParticleResolution(Status status, Particle particle) { this.status = status; this.particle = particle; }
    public static ClassicParticleResolution resolved(Particle particle) { return new ClassicParticleResolution(Status.RESOLVED, particle); }
    public static ClassicParticleResolution failed(Status status) { return new ClassicParticleResolution(status, null); }
    public Status getStatus() { return status; }
    public Optional<Particle> getParticle() { return Optional.ofNullable(particle); }
    public boolean isResolved() { return status == Status.RESOLVED; }
}
