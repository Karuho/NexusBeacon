package cl.dynasty.nexusbeacon.platform.api;

import org.bukkit.inventory.ItemStack;

public interface ItemIdentityService {

    IdentificationResult identify(ItemStack item);

    ItemStack mark(ItemStack item, ItemIdentity identity);
}
