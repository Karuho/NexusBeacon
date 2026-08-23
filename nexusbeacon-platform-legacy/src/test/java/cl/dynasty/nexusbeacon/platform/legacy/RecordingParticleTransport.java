package cl.dynasty.nexusbeacon.platform.legacy;

import org.bukkit.World;
import org.bukkit.entity.Player;

final class RecordingParticleTransport implements LegacyParticleTransport {
    int playerCalls;
    int worldCalls;
    int lastRadius;
    LegacyParticleResolution lastResolution;
    LegacyParticleRequest lastRequest;

    @Override public void emit(Player player, LegacyParticleResolution resolution,
            LegacyParticleRequest request, int radius) {
        playerCalls++;
        record(resolution, request, radius);
    }

    @Override public void emit(World world, LegacyParticleResolution resolution,
            LegacyParticleRequest request, int radius) {
        worldCalls++;
        record(resolution, request, radius);
    }

    private void record(LegacyParticleResolution resolution, LegacyParticleRequest request, int radius) {
        lastResolution = resolution;
        lastRequest = request;
        lastRadius = radius;
    }
}
