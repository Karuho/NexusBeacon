package cl.dynasty.nexusbeacon.listener;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import cl.dynasty.nexusbeacon.NexusBeaconPlugin;
import cl.dynasty.nexusbeacon.model.BeaconData;

public class BeaconListener implements Listener {

    private final NexusBeaconPlugin plugin;

    public BeaconListener(NexusBeaconPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBeaconPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();

        if (block.getType() != Material.BEACON) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = event.getItemInHand();

        if (!plugin.getCustomBeaconItemManager().isCustomBeacon(item)) {
            return; // beacon vanilla: no lo tocamos
        }

        if (!player.hasPermission("NexusBeacon.use")) {
            event.setCancelled(true);
            player.sendMessage(plugin.getLanguageManager().withPrefix("beacon.place-no-permission"));
            return;
        }

        String uniqueId = plugin.getCustomBeaconItemManager().readUniqueId(item);
        java.util.Map<String, Integer> effects = plugin.getCustomBeaconItemManager().readEffects(item);
        java.util.Set<String> activeEffects = plugin.getCustomBeaconItemManager().readActiveEffects(item);

        plugin.getBeaconManager().registerBeacon(
                block.getLocation(),
                player.getUniqueId(),
                uniqueId,
                effects,
                activeEffects);

        player.sendMessage(plugin.getLanguageManager().withPrefix("beacon.registered"));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBeaconInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) {
            return;
        }

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block block = event.getClickedBlock();

        if (block.getType() != Material.BEACON) {
            return;
        }

        BeaconData beacon = plugin.getBeaconManager().getBeacon(block.getLocation());

        if (beacon == null) {
            return;
        }

        if (plugin.getConfigManager().getBeaconConfig().getBoolean("protection.owner-only-interact", true)
                && !beacon.canManage(event.getPlayer().getUniqueId())
                && !event.getPlayer().hasPermission("NexusBeacon.admin")) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(plugin.getLanguageManager().withPrefix("beacon.not-owner"));
            return;
        }

        event.setCancelled(true);
        plugin.getBeaconGuiManager().openMainMenu(event.getPlayer(), beacon);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBeaconBreak(BlockBreakEvent event) {
        Block block = event.getBlock();

        if (block.getType() != Material.BEACON) {
            return;
        }

        BeaconData beacon = plugin.getBeaconManager().getBeacon(block.getLocation());

        if (beacon == null) {
            return;
        }

        if (plugin.getConfigManager().getBeaconConfig().getBoolean("protection.owner-only-break", true)
                && !beacon.canManage(event.getPlayer().getUniqueId())
                && !event.getPlayer().hasPermission("NexusBeacon.admin")) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(plugin.getLanguageManager().withPrefix("beacon.break-not-owner"));
            return;
        }

        event.setDropItems(false);

        org.bukkit.inventory.ItemStack dropItem = plugin.getCustomBeaconItemManager().createBeaconItemFromData(beacon);

        boolean creativeNoDupe = plugin.getConfigManager()
                .getBeaconConfig()
                .getBoolean("beacon-item.creative-no-dupe", true);

        boolean cancelIfFull = plugin.getConfigManager()
                .getBeaconConfig()
                .getBoolean("beacon-item.cancel-if-inventory-full", true);

        boolean autoPickup = plugin.getConfigManager()
                .getBeaconConfig()
                .getBoolean("beacon-item.auto-pickup", true);

        if (event.getPlayer().getGameMode() == org.bukkit.GameMode.CREATIVE && creativeNoDupe) {
    plugin.getBeaconManager().removeBeacon(block.getLocation());
    event.getPlayer().sendMessage(plugin.getLanguageManager().withPrefix("beacon.removed"));
    return;
}

        if (autoPickup && cancelIfFull && event.getPlayer().getInventory().firstEmpty() == -1) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(plugin.getLanguageManager().withPrefix("beacon.inventory-full"));
            return;
        }

        plugin.getBeaconManager().removeBeacon(block.getLocation());

        if (autoPickup) {
            event.getPlayer().getInventory().addItem(dropItem);
        } else {
            block.getWorld().dropItemNaturally(block.getLocation(), dropItem);
        }

        event.getPlayer().sendMessage(plugin.getLanguageManager().withPrefix("beacon.removed"));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBaseBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();

        if (block.getType() == Material.BEACON) {
            return;
        }

        if (!plugin.getConfigManager().getBeaconConfig().getBoolean("protection.protect-base-blocks", true)) {
            return;
        }

        BeaconData beacon = plugin.getBeaconPowerManager().getBeaconByBaseBlock(block);

        if (beacon == null) {
            return;
        }

        if (beacon.canManage(event.getPlayer().getUniqueId())
                || event.getPlayer().hasPermission("NexusBeacon.admin")) {
            return;
        }

        event.setCancelled(true);
        event.getPlayer().sendMessage(plugin.getLanguageManager().withPrefix("beacon.base-block-protected"));
    }
}