package cl.dynasty.nexusbeacon.gui.framework;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

@FunctionalInterface
public interface NexusGuiAction {

    void execute(Player player, NexusGuiMenu menu, InventoryClickEvent event);
}