package cl.dynasty.nexusbeacon.platform.legacy;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;

/** Safe InventoryClick/Drag interaction path shared by Spigot 1.8.8 and 1.12.2. */
public final class LegacyGuiInteractionListener implements Listener {
    private final LegacyApplicationState state;
    private final LegacyGuiController controller;

    public LegacyGuiInteractionListener(LegacyApplicationState state, LegacyGuiController controller) {
        if (state == null || controller == null) throw new NullPointerException();
        this.state = state;
        this.controller = controller;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null) return;
        LegacyBeaconState beacon;
        try {
            beacon = state.find(new LegacyBeaconLocation(block.getWorld().getName(),
                    block.getX(), block.getY(), block.getZ()));
        } catch (IllegalArgumentException invalidWorld) {
            return;
        }
        if (beacon == null) return;
        event.setCancelled(true);
        controller.open(event.getPlayer(), beacon);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        LegacyGuiHolder holder = controller.holder(top);
        if (holder == null || !(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        LegacyGuiSession session = holder.getSession();
        if (!session.getPlayerId().equals(player.getUniqueId())) {
            event.setCancelled(true);
            player.closeInventory();
            return;
        }
        int rawSlot = event.getRawSlot();
        String action = event.getAction().name();
        String click = event.getClick().name();
        boolean topSlot = rawSlot >= 0 && rawSlot < top.getSize();
        boolean transfer = "MOVE_TO_OTHER_INVENTORY".equals(action)
                || "COLLECT_TO_CURSOR".equals(action)
                || "HOTBAR_SWAP".equals(action)
                || "HOTBAR_MOVE_AND_READD".equals(action)
                || "DOUBLE_CLICK".equals(click)
                || "NUMBER_KEY".equals(click);
        if (!topSlot && !transfer) return;
        event.setCancelled(true);
        if (!topSlot || controller.currentAuthorized(player, session) == null) {
            if (controller.currentAuthorized(player, session) == null) player.closeInventory();
            return;
        }
        if (!isSemanticClick(click)) return;
        LegacyBeaconState current = controller.currentAuthorized(player, session);
        if (session.getMenu() == LegacyGuiMenu.MAIN) {
            if (rawSlot == 20) controller.open(player, current, LegacyGuiMenu.EFFECTS);
            else if (rawSlot == 24) controller.open(player, current, LegacyGuiMenu.BEAM_STYLES);
            else if (rawSlot == 49) player.closeInventory();
            return;
        }
        if (rawSlot == 49) {
            controller.open(player, current, LegacyGuiMenu.MAIN);
            return;
        }
        if (session.getMenu() == LegacyGuiMenu.EFFECTS) {
            String effectId = controller.effectAt(rawSlot);
            if (effectId == null) return;
            if ("RIGHT".equals(click)) {
                player.sendMessage("\u00a7ePaid acquisition and upgrades are unavailable on Legacy.");
                return;
            }
            LegacyGuiMutationResult result = controller.toggleEffect(player, session, effectId);
            feedback(player, result);
            if (result == LegacyGuiMutationResult.COMMITTED) {
                controller.open(player, state.findByUniqueId(session.getBeaconId()), LegacyGuiMenu.EFFECTS);
            }
            return;
        }
        String styleId = controller.beamStyleAt(rawSlot);
        if (styleId == null) return;
        if (!"LEFT".equals(click)) return;
        LegacyGuiMutationResult result = controller.selectBeamStyle(player, session, styleId);
        feedback(player, result);
        if (result == LegacyGuiMutationResult.COMMITTED) {
            controller.open(player, state.findByUniqueId(session.getBeaconId()), LegacyGuiMenu.BEAM_STYLES);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDrag(InventoryDragEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (controller.holder(top) == null) return;
        for (Integer rawSlot : event.getRawSlots()) {
            if (rawSlot.intValue() < top.getSize()) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler public void onClose(InventoryCloseEvent event) {
        LegacyGuiHolder holder = controller.holder(event.getView().getTopInventory());
        if (holder != null) controller.close(holder.getSession());
    }

    @EventHandler public void onQuit(PlayerQuitEvent event) { controller.quit(event.getPlayer().getUniqueId()); }

    private static void feedback(Player player, LegacyGuiMutationResult result) {
        if (result == LegacyGuiMutationResult.COMMITTED) player.sendMessage("\u00a7aNexusBeacon updated.");
        else if (result == LegacyGuiMutationResult.NOT_ACQUIRED) {
            player.sendMessage("\u00a7eThis effect is not acquired; paid acquisition is unavailable on Legacy.");
        } else if (result == LegacyGuiMutationResult.UNSUPPORTED) {
            player.sendMessage("\u00a7cThis option is unavailable on this Legacy server.");
        } else player.sendMessage("\u00a7cThe GUI action failed safely: " + result.name().toLowerCase());
    }

    static boolean isSemanticClick(String click) {
        return "LEFT".equals(click) || "RIGHT".equals(click);
    }
}
