package cl.dynasty.nexusbeacon.gui.framework;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public final class NexusGuiMenu {

    private final String id;
    private final String title;
    private final int size;
    private final Map<Integer, NexusGuiItem> items = new HashMap<>();
    private final NexusPlaceholderContext context;

    public NexusGuiMenu(String id, String title, int size, NexusPlaceholderContext context) {
        this.id = id;
        this.title = NexusItemBuilder.color(context.apply(title));
        this.size = normalizeSize(size);
        this.context = context;
    }

    public List<Integer> getSlotsBySymbol(char symbol, List<String> layout) {
    List<Integer> slots = new ArrayList<>();

    for (int row = 0; row < layout.size(); row++) {
        String line = layout.get(row);

        for (int column = 0; column < Math.min(9, line.length()); column++) {
            if (line.charAt(column) == symbol) {
                slots.add(row * 9 + column);
            }
        }
    }

    return slots;
}

    public String getId() {
        return id;
    }

    public NexusGuiItem getItem(int slot) {
        return items.get(slot);
    }

    public NexusPlaceholderContext getContext() {
        return context;
    }

    public void setItem(int slot, NexusGuiItem item) {
        if (slot < 0 || slot >= size || item == null) {
            return;
        }

        items.put(slot, item);
    }

    public void open(Player player) {
        NexusGuiHolder holder = new NexusGuiHolder(this);
        Inventory inventory = Bukkit.createInventory(holder, size, title);
        holder.setInventory(inventory);

        for (Map.Entry<Integer, NexusGuiItem> entry : items.entrySet()) {
            inventory.setItem(entry.getKey(), entry.getValue().getItemStack());
        }

        player.openInventory(inventory);
    }

    private int normalizeSize(int size) {
        if (size < 9) {
            return 9;
        }

        if (size > 54) {
            return 54;
        }

        return ((size + 8) / 9) * 9;
    }
}