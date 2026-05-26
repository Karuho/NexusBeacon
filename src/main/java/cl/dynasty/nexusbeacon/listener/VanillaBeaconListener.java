package cl.dynasty.nexusbeacon.listener;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

import cl.dynasty.nexusbeacon.NexusBeaconPlugin;
import cl.dynasty.nexusbeacon.util.ColorUtil;

public class VanillaBeaconListener implements Listener {

    private final NexusBeaconPlugin plugin;

    public VanillaBeaconListener(NexusBeaconPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onVanillaBeaconPlace(BlockPlaceEvent event) {
        if (event.getBlock().getType() != Material.BEACON) {
            return;
        }

        if (plugin.getCustomBeaconItemManager().isCustomBeacon(event.getItemInHand())) {
            return;
        }

        if (!plugin.getConfigManager().getBeaconConfig().getBoolean("vanilla-beacon.disable-vanilla", false)) {
            return;
        }

        event.setCancelled(true);
        event.getPlayer().sendMessage(ColorUtil.color("&b[NexusBeacon]&r &cLos beacons vanilla están deshabilitados."));
    }
}