package cl.dynasty.nexusbeacon.platform.legacy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.Material;

/** Immutable physical plan used before touching Bukkit ItemMeta. */
public final class LegacyGuiItemPlan {
    private final Material material;
    private final short data;
    private final String displayName;
    private final List<String> lore;
    private final boolean visualFallback;
    private final boolean customModelDataOmitted;
    private final String skullOwner;

    LegacyGuiItemPlan(Material material, short data, String displayName, List<String> lore,
            boolean visualFallback, boolean customModelDataOmitted, String skullOwner) {
        this.material = material;
        this.data = data;
        this.displayName = displayName;
        this.lore = Collections.unmodifiableList(new ArrayList<String>(lore));
        this.visualFallback = visualFallback;
        this.customModelDataOmitted = customModelDataOmitted;
        this.skullOwner = skullOwner;
    }

    public Material getMaterial() { return material; }
    public short getData() { return data; }
    public String getDisplayName() { return displayName; }
    public List<String> getLore() { return lore; }
    public boolean isVisualFallback() { return visualFallback; }
    public boolean isCustomModelDataOmitted() { return customModelDataOmitted; }
    public String getSkullOwner() { return skullOwner; }
}
