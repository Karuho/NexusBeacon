package cl.dynasty.nexusbeacon.platform.legacy;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/** Holder identity is authoritative; titles and item presentation are not. */
public final class LegacyGuiHolder implements InventoryHolder {
    private final Object controllerIdentity;
    private final LegacyGuiSession session;
    private Inventory inventory;

    LegacyGuiHolder(Object controllerIdentity, LegacyGuiSession session) {
        if (controllerIdentity == null || session == null) throw new NullPointerException();
        this.controllerIdentity = controllerIdentity;
        this.session = session;
    }

    boolean belongsTo(Object identity) { return controllerIdentity == identity; }
    public LegacyGuiSession getSession() { return session; }
    void setInventory(Inventory inventory) { this.inventory = inventory; }
    @Override public Inventory getInventory() { return inventory; }
}
