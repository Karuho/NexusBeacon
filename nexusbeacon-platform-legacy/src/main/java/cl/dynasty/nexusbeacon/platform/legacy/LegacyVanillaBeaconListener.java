package cl.dynasty.nexusbeacon.platform.legacy;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

import cl.dynasty.nexusbeacon.platform.api.MaterialContext;

/** Legacy equivalent of the storage-independent vanilla-beacon placement policy. */
public final class LegacyVanillaBeaconListener implements Listener {
    private final LegacyItemIdentityService identities;
    private final LegacyMessageService messages;
    private final Material beaconMaterial;
    private final boolean vanillaDisabled;
    private final String disabledMessage;

    public LegacyVanillaBeaconListener(LegacyItemIdentityService identities,
            LegacyMaterialResolver materials, LegacyMessageService messages,
            boolean vanillaDisabled, String disabledMessage) {
        if (identities == null) throw new NullPointerException("identities");
        if (materials == null) throw new NullPointerException("materials");
        if (messages == null) throw new NullPointerException("messages");
        if (disabledMessage == null || disabledMessage.trim().isEmpty()) {
            throw new IllegalArgumentException("disabledMessage is required");
        }
        this.identities = identities;
        this.messages = messages;
        this.beaconMaterial = materials.resolveMaterial("BEACON", MaterialContext.REQUIRED_ITEM)
                .getMaterial().orElseThrow(new java.util.function.Supplier<IllegalStateException>() {
                    @Override public IllegalStateException get() {
                        return new IllegalStateException("BEACON material is unavailable");
                    }
                });
        this.vanillaDisabled = vanillaDisabled;
        this.disabledMessage = disabledMessage;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onVanillaBeaconPlace(BlockPlaceEvent event) {
        if (event == null || event.getBlock() == null || event.getBlock().getType() != beaconMaterial) return;
        ItemStack item = event.getItemInHand();
        if (item != null && identities.identify(item).isRecognized()) return;
        if (!vanillaDisabled) return;
        event.setCancelled(true);
        messages.sendChat(event.getPlayer(), disabledMessage);
    }
}
