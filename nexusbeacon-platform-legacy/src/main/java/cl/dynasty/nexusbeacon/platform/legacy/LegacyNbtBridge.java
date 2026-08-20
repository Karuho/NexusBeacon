package cl.dynasty.nexusbeacon.platform.legacy;

import java.util.Optional;
import org.bukkit.inventory.ItemStack;

public interface LegacyNbtBridge {
    ItemStack mark(ItemStack item);
    LegacyIdentityStatus identify(ItemStack item);
    default ItemStack writePortableData(ItemStack item, LegacyPortableBeaconData data) {
        throw new UnsupportedOperationException("Portable beacon data is unavailable");
    }
    default Optional<LegacyPortableBeaconData> readPortableData(ItemStack item) {
        return Optional.empty();
    }
    String getRevision();
}
