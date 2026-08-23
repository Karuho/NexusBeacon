package cl.dynasty.nexusbeacon.platform.legacy;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/** Creates chest-style inventories with the strict limits shared by 1.8.8 and 1.12.2. */
public final class LegacyInventoryFactory {
    public static final int MIN_SIZE = 9;
    public static final int MAX_SIZE = 54;
    public static final int MAX_TITLE_LENGTH = 32;

    private final InventoryCreator creator;

    public LegacyInventoryFactory() {
        this(new InventoryCreator() {
            @Override public Inventory create(InventoryHolder holder, int size, String title) {
                return Bukkit.createInventory(holder, size, title);
            }
        });
    }

    LegacyInventoryFactory(InventoryCreator creator) {
        if (creator == null) throw new NullPointerException("creator");
        this.creator = creator;
    }

    public Inventory create(InventoryHolder holder, int size, String title) {
        validate(size, title);
        return creator.create(holder, size, title);
    }

    public static void validate(int size, String title) {
        if (size < MIN_SIZE || size > MAX_SIZE || size % 9 != 0) {
            throw new IllegalArgumentException("Legacy chest inventory size must be a multiple of 9 from 9 to 54: "
                    + size);
        }
        if (title == null) throw new IllegalArgumentException("Legacy inventory title cannot be null");
        if (title.length() > MAX_TITLE_LENGTH) {
            throw new IllegalArgumentException("Legacy inventory title exceeds 32 characters: " + title.length());
        }
    }

    interface InventoryCreator {
        Inventory create(InventoryHolder holder, int size, String title);
    }
}
