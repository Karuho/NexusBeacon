package cl.dynasty.nexusbeacon.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class NexusBeaconGuiHolder implements InventoryHolder {

    private final String menuId;
    private Inventory inventory;

    public NexusBeaconGuiHolder(String menuId) {
        this.menuId = menuId;
    }

    public String getMenuId() {
        return menuId;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}