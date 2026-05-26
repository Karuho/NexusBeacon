package cl.dynasty.nexusbeacon.gui.framework;

import org.bukkit.inventory.ItemStack;

public final class NexusGuiItem {

    private final ItemStack itemStack;
    private final NexusGuiAction action;

    public NexusGuiItem(ItemStack itemStack, NexusGuiAction action) {
        this.itemStack = itemStack;
        this.action = action;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public NexusGuiAction getAction() {
        return action;
    }
}