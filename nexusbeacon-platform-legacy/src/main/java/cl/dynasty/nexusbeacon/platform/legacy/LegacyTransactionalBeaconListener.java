package cl.dynasty.nexusbeacon.platform.legacy;

import java.util.Map;
import java.util.UUID;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import cl.dynasty.nexusbeacon.platform.api.IdentificationResult;

/** Productive marked placement/removal subset. Interaction/GUI/effects remain deferred. */
public final class LegacyTransactionalBeaconListener implements Listener {
    private final Plugin plugin;
    private final LegacyApplicationState state;
    private final LegacyItemIdentityService identities;
    private final LegacyBeaconTransactionService transactions;
    private final LegacyBeaconItemFactory items;
    private final LegacyBeaconGameplaySettings settings;
    private final LegacyMessageService messages;
    private final LegacyBeaconListenerMessages text;
    private final Material beaconMaterial;

    public LegacyTransactionalBeaconListener(Plugin plugin, LegacyApplicationGraph application,
            LegacyBeaconGameplaySettings settings, LegacyBeaconItemFactory items,
            LegacyBeaconListenerMessages text) {
        this(plugin, application.getState(), application.getIdentities(),
                new LegacyBeaconTransactionService(application.getState(), application.getIdentities(), settings),
                items, settings, application.getMessages(), text,
                application.getMaterials()
                        .resolveMaterial("BEACON",
                                cl.dynasty.nexusbeacon.platform.api.MaterialContext.REQUIRED_ITEM)
                        .getMaterial().orElse(Material.BEACON));
    }

    LegacyTransactionalBeaconListener(Plugin plugin, LegacyApplicationState state,
            LegacyItemIdentityService identities, LegacyBeaconTransactionService transactions,
            LegacyBeaconItemFactory items, LegacyBeaconGameplaySettings settings,
            LegacyMessageService messages, LegacyBeaconListenerMessages text, Material beaconMaterial) {
        if (plugin == null) throw new NullPointerException("plugin");
        if (state == null) throw new NullPointerException("state");
        if (identities == null) throw new NullPointerException("identities");
        if (transactions == null) throw new NullPointerException("transactions");
        if (settings == null) throw new NullPointerException("settings");
        if (messages == null) throw new NullPointerException("messages");
        if (text == null) throw new NullPointerException("text");
        if (beaconMaterial == null) throw new NullPointerException("beaconMaterial");
        this.plugin = plugin;
        this.state = state;
        this.identities = identities;
        this.transactions = transactions;
        this.items = items;
        this.settings = settings;
        this.messages = messages;
        this.text = text;
        this.beaconMaterial = beaconMaterial;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBeaconPlace(final BlockPlaceEvent event) {
        final Block block = event.getBlock();
        if (block == null || block.getType() != beaconMaterial) return;
        ItemStack item = event.getItemInHand();
        IdentificationResult identity = identities.identify(item);
        if (identity.getStatus() == IdentificationResult.Status.NOT_RECOGNIZED) return;
        if (!identity.isRecognized()) {
            event.setCancelled(true);
            messages.sendChat(event.getPlayer(), text.getInvalidItem());
            return;
        }
        Player player = event.getPlayer();
        if (!event.canBuild() || player == null || !player.hasPermission("NexusBeacon.use")) {
            event.setCancelled(true);
            messages.sendChat(player, text.getNoPlacePermission());
            return;
        }
        final LegacyBeaconLocation location;
        try {
            location = location(block);
        } catch (IllegalArgumentException invalidLocation) {
            event.setCancelled(true);
            messages.sendChat(player, text.getTransactionFailed());
            return;
        }
        LegacyBeaconTransactionResult result = transactions.place(item, location, player.getUniqueId());
        if (!result.isCommitted()) {
            event.setCancelled(true);
            messages.sendChat(player, result.getStatus() == LegacyBeaconTransactionStatus.DUPLICATE_LOCATION_OR_ID
                    ? text.getDuplicate() : result.getStatus() == LegacyBeaconTransactionStatus.MALFORMED_ITEM
                    ? text.getInvalidItem() : text.getTransactionFailed());
            return;
        }
        final Player committedPlayer = player;
        plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
            @Override public void run() {
                if (block.getType() == beaconMaterial && state.find(location) != null) {
                    messages.sendChat(committedPlayer, text.getPlaced());
                    return;
                }
                try {
                    state.delete(location);
                } catch (RuntimeException recoveryFailure) {
                    failHard("Placed block rollback could not restore authoritative state", recoveryFailure);
                }
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBeaconBreak(BlockBreakEvent event) {
        final Block block = event.getBlock();
        if (block == null) return;
        final LegacyBeaconLocation location;
        try {
            location = location(block);
        } catch (IllegalArgumentException invalidLocation) {
            return;
        }
        LegacyBeaconState current = state.find(location);
        if (current == null) return;
        event.setCancelled(true);
        if (block.getType() != beaconMaterial) {
            messages.sendChat(event.getPlayer(), text.getTransactionFailed());
            return;
        }
        Player player = event.getPlayer();
        UUID playerId = player == null ? null : player.getUniqueId();
        if (settings.isOwnerOnlyBreak() && !current.getTrustedPlayers().contains(playerId)
                && (current.getOwner() == null || !current.getOwner().equals(playerId))
                && (player == null || !player.hasPermission("NexusBeacon.admin"))) {
            messages.sendChat(player, text.getNotOwner());
            return;
        }
        boolean returnItem = player == null || player.getGameMode() != GameMode.CREATIVE
                || !settings.isCreativeNoDupe();
        ItemStack returned = returnItem ? requiredItemFactory().createFromState(current) : null;
        if (returned != null && settings.isAutoPickup() && settings.isCancelIfInventoryFull()
                && player.getInventory().firstEmpty() == -1) {
            messages.sendChat(player, text.getInventoryFull());
            return;
        }
        LegacyBeaconTransactionResult result;
        try {
            result = transactions.remove(location, new LegacyWorldBeaconMutation() {
                @Override public boolean isBeacon() { return block.getType() == beaconMaterial; }
                @Override public void removeBeacon() { block.setType(Material.AIR); }
            });
        } catch (LegacyStorageException unrecoverable) {
            failHard("Beacon removal recovery failed", unrecoverable);
            return;
        }
        if (!result.isCommitted()) {
            messages.sendChat(player, text.getTransactionFailed());
            return;
        }
        if (returned != null) deliver(player, block, returned);
        messages.sendChat(player, text.getRemoved());
    }

    private LegacyBeaconItemFactory requiredItemFactory() {
        if (items == null) throw new IllegalStateException("Legacy beacon item factory is unavailable");
        return items;
    }

    private void deliver(Player player, Block block, ItemStack returned) {
        if (settings.isAutoPickup() && player != null) {
            Map<Integer, ItemStack> leftovers = player.getInventory().addItem(returned);
            for (ItemStack leftover : leftovers.values()) {
                block.getWorld().dropItemNaturally(block.getLocation(), leftover);
            }
        } else {
            block.getWorld().dropItemNaturally(block.getLocation(), returned);
        }
    }

    private LegacyBeaconLocation location(Block block) {
        return new LegacyBeaconLocation(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
    }

    private void failHard(String message, RuntimeException failure) {
        plugin.getLogger().severe(message + ": " + failure.getMessage());
        plugin.getServer().getPluginManager().disablePlugin(plugin);
    }
}
