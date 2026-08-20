package cl.dynasty.nexusbeacon.platform.legacy;

import org.bukkit.World;
import org.bukkit.entity.Player;

interface LegacyParticleTransport {
    void emit(Player player, LegacyParticleResolution resolution, LegacyParticleRequest request, int radius);
    void emit(World world, LegacyParticleResolution resolution, LegacyParticleRequest request, int radius);
}
