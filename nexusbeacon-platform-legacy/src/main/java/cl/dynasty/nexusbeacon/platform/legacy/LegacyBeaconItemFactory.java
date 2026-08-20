package cl.dynasty.nexusbeacon.platform.legacy;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import cl.dynasty.nexusbeacon.platform.api.ItemIdentity;
import cl.dynasty.nexusbeacon.platform.api.MaterialContext;

/** Productive Legacy item construction path; visible metadata is never used as identity. */
public final class LegacyBeaconItemFactory {
    private final LegacyItemIdentityService identities;
    private final LegacyTextFormatter formatter;
    private final Material beaconMaterial;
    private final String displayName;
    private final List<String> freshLore;

    public LegacyBeaconItemFactory(LegacyItemIdentityService identities, LegacyMaterialResolver materials,
            LegacyTextFormatter formatter, String displayName) {
        this(identities, materials, formatter, displayName,
                java.util.Collections.singletonList("&7Portable NexusBeacon"));
    }

    public LegacyBeaconItemFactory(LegacyItemIdentityService identities, LegacyMaterialResolver materials,
            LegacyTextFormatter formatter, String displayName, List<String> freshLore) {
        if (identities == null) throw new NullPointerException("identities");
        if (materials == null) throw new NullPointerException("materials");
        if (formatter == null) throw new NullPointerException("formatter");
        if (displayName == null || displayName.trim().isEmpty()) {
            throw new IllegalArgumentException("displayName is required");
        }
        this.identities = identities;
        this.formatter = formatter;
        this.beaconMaterial = materials.resolveMaterial("BEACON", MaterialContext.REQUIRED_ITEM)
                .getMaterial().orElseThrow(new java.util.function.Supplier<IllegalStateException>() {
                    @Override public IllegalStateException get() {
                        return new IllegalStateException("BEACON material is unavailable");
                    }
                });
        this.displayName = displayName;
        if (freshLore == null) throw new NullPointerException("freshLore");
        this.freshLore = new ArrayList<String>(freshLore);
    }

    public ItemStack createNew(int amount) {
        if (amount <= 0 || amount > beaconMaterial.getMaxStackSize()) {
            throw new IllegalArgumentException("Invalid NexusBeacon item amount");
        }
        return identities.mark(decorate(new ItemStack(beaconMaterial, amount), null),
                ItemIdentity.NEXUS_BEACON);
    }

    public ItemStack createFromState(LegacyBeaconState beacon) {
        if (beacon == null) throw new NullPointerException("beacon");
        ItemStack item = decorate(new ItemStack(beaconMaterial, 1), beacon);
        return identities.markPortable(item, new LegacyPortableBeaconData(beacon.getUniqueId(),
                beacon.getEffectLevels(), beacon.getActiveEffects()));
    }

    private ItemStack decorate(ItemStack item, LegacyBeaconState beacon) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(formatter.format(displayName));
        List<String> lore = new ArrayList<String>();
        if (beacon == null) {
            for (String line : freshLore) lore.add(formatter.format(line));
        } else {
            lore.add(formatter.format("&7Owner: &f" + (beacon.getOwner() == null ? "None" : beacon.getOwner())));
            lore.add(formatter.format("&7Range: &f" + beacon.getRange()));
            lore.add(formatter.format("&7Effects: &f" + beacon.getEffectLevels().size()));
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
}
