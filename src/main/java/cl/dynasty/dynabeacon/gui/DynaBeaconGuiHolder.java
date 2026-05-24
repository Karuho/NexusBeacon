package cl.dynasty.dynabeacon.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class DynaBeaconGuiHolder implements InventoryHolder {

    private final String menuId;
    private Inventory inventory;

    public DynaBeaconGuiHolder(String menuId) {
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