package cl.dynasty.nexusbeacon.gui.framework;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class NexusGuiHolder implements InventoryHolder {

    private final NexusGuiMenu menu;
    private Inventory inventory;

    public NexusGuiHolder(NexusGuiMenu menu) {
        this.menu = menu;
    }

    public NexusGuiMenu getMenu() {
        return menu;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}