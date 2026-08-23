package cl.dynasty.nexusbeacon.platform.legacy;

import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

/** Exact product base-break protection; explosions and pistons are not product behaviors. */
public final class LegacyBaseProtectionListener implements Listener {
    private final LegacyApplicationState state;
    private final LegacyMessageService messages;
    private final boolean globallyEnabled;
    private final int maxLayers;
    private final String protectedMessage;

    public LegacyBaseProtectionListener(LegacyApplicationState state, LegacyMessageService messages,
            boolean globallyEnabled, int maxLayers, String protectedMessage) {
        if (state == null || messages == null || protectedMessage == null) throw new NullPointerException();
        if (maxLayers < 1) throw new IllegalArgumentException("maxLayers must be positive");
        this.state = state;
        this.messages = messages;
        this.globallyEnabled = globallyEnabled;
        this.maxLayers = maxLayers;
        this.protectedMessage = protectedMessage;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBaseBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!globallyEnabled || block == null || block.getType() == Material.BEACON) return;
        LegacyBeaconState beacon = findProtectingBeacon(block);
        if (beacon == null) return;
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        if (playerId.equals(beacon.getOwner()) || beacon.getTrustedPlayers().contains(playerId)
                || player.hasPermission("NexusBeacon.admin")) return;
        event.setCancelled(true);
        messages.sendChat(player, protectedMessage);
    }

    LegacyBeaconState findProtectingBeacon(Block block) {
        if (!globallyEnabled || block == null || block.getWorld() == null) return null;
        String world = block.getWorld().getName();
        for (int layer = 1; layer <= maxLayers; layer++) {
            int beaconY = block.getY() + layer;
            for (int x = block.getX() - layer; x <= block.getX() + layer; x++) {
                for (int z = block.getZ() - layer; z <= block.getZ() + layer; z++) {
                    LegacyBeaconState candidate = state.find(new LegacyBeaconLocation(world, x, beaconY, z));
                    if (candidate != null && candidate.isProtectBaseBlocks()) return candidate;
                }
            }
        }
        return null;
    }
}
