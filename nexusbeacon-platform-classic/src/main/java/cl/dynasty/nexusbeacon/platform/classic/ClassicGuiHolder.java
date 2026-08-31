package cl.dynasty.nexusbeacon.platform.classic;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

final class ClassicGuiHolder implements InventoryHolder {
    enum Menu { MAIN, EFFECTS, BEAM_STYLES, PAYMENT }
    private final ClassicBeaconLocation location;
    private final Menu menu;
    private final String effectId;
    private Inventory inventory;
    ClassicGuiHolder(ClassicBeaconLocation location, Menu menu) { this(location,menu,null); }
    ClassicGuiHolder(ClassicBeaconLocation location, Menu menu,String effectId) { this.location=location; this.menu=menu; this.effectId=effectId; }
    ClassicBeaconLocation getLocation(){return location;}
    Menu getMenu(){return menu;}
    String getEffectId(){return effectId;}
    void setInventory(Inventory inventory){this.inventory=inventory;}
    public Inventory getInventory(){return inventory;}
}
