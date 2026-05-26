package cl.dynasty.nexusbeacon.gui.framework;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

public final class NexusGuiListener implements Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    public void onClick(InventoryClickEvent event) {
        Inventory topInventory = event.getView().getTopInventory();

        if (!(topInventory.getHolder() instanceof NexusGuiHolder holder)) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (event.getClickedInventory() == null || event.getClickedInventory() != topInventory) {
            return;
        }

        NexusGuiMenu menu = holder.getMenu();
        NexusGuiItem item = menu.getItem(event.getRawSlot());

        if (item == null || item.getAction() == null) {
            return;
        }

        item.getAction().execute(player, menu, event);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDrag(InventoryDragEvent event) {
        Inventory topInventory = event.getView().getTopInventory();

        if (topInventory.getHolder() instanceof NexusGuiHolder) {
            event.setCancelled(true);
        }
    }
}