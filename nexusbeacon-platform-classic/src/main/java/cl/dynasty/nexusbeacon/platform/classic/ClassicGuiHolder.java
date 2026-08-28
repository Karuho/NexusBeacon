package cl.dynasty.nexusbeacon.platform.classic;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

final class ClassicGuiHolder implements InventoryHolder {
    enum Menu { MAIN, EFFECTS, BEAM_STYLES }
    private final ClassicBeaconLocation location;
    private final Menu menu;
    private Inventory inventory;
    ClassicGuiHolder(ClassicBeaconLocation location, Menu menu) { this.location=location; this.menu=menu; }
    ClassicBeaconLocation getLocation(){return location;}
    Menu getMenu(){return menu;}
    void setInventory(Inventory inventory){this.inventory=inventory;}
    public Inventory getInventory(){return inventory;}
}
