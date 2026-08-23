package cl.dynasty.nexusbeacon.platform.legacy;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/** Stable Bukkit 1.8 command lookups backed by the authoritative registry. */
public final class LegacyBukkitCommandEnvironment implements LegacyCommandEnvironment {
    private final LegacyApplicationState state;

    public LegacyBukkitCommandEnvironment(LegacyApplicationState state) {
        if (state == null) throw new NullPointerException("state");
        this.state = state;
    }

    @Override public Player findOnlinePlayer(String name) { return Bukkit.getPlayer(name); }

    @Override public LegacyBeaconState findTargetBeacon(Player player) {
        if (player == null || !state.getStatus().isReady()) return null;
        Block block = targetBlock(player, 6);
        if (block == null || block.getType() != Material.BEACON || block.getWorld() == null) return null;
        try {
            return state.find(new LegacyBeaconLocation(block.getWorld().getName(),
                    block.getX(), block.getY(), block.getZ()));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private Block targetBlock(Player player, int maximumDistance) {
        Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection().normalize();
        Block last = eye.getBlock();
        for (double distance = 0.0D; distance <= maximumDistance; distance += 0.2D) {
            Block candidate = eye.clone().add(direction.clone().multiply(distance)).getBlock();
            last = candidate;
            if (candidate.getType() != Material.AIR) return candidate;
        }
        return last;
    }
}
