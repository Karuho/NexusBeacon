package cl.dynasty.nexusbeacon.platform.legacy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import cl.dynasty.nexusbeacon.platform.api.MaterialContext;
import cl.dynasty.nexusbeacon.platform.api.MaterialResolution;

/** Builds transient Legacy GUI items without attaching NexusBeacon identity. */
public final class LegacyGuiItemFactory {
    private static final String[] VISUAL_FLAGS = {
            "HIDE_ATTRIBUTES", "HIDE_ENCHANTS", "HIDE_UNBREAKABLE"
    };

    private final LegacyMaterialResolver materials;
    private final LegacyTextFormatter text;

    public LegacyGuiItemFactory(LegacyMaterialResolver materials, LegacyTextFormatter text) {
        if (materials == null) throw new NullPointerException("materials");
        if (text == null) throw new NullPointerException("text");
        this.materials = materials;
        this.text = text;
    }

    public LegacyGuiItemPlan planItem(String materialId, MaterialContext context, String displayName,
            List<String> lore, Integer customModelData) {
        LegacyMaterialResolution legacy = materials.resolveLegacyMaterial(materialId, context);
        MaterialResolution resolution = legacy.getResolution();
        Material material = resolution.getMaterial().orElseThrow(new java.util.function.Supplier<IllegalArgumentException>() {
            @Override public IllegalArgumentException get() {
                return new IllegalArgumentException("Unsupported Legacy GUI material: " + materialId
                        + " in context " + context);
            }
        });
        return new LegacyGuiItemPlan(material, legacy.getData(), text.format(displayName),
                formatLore(lore), resolution.isFallbackUsed(), customModelData != null, null);
    }

    public ItemStack createItem(String materialId, MaterialContext context, String displayName,
            List<String> lore, Integer customModelData) {
        LegacyGuiItemPlan plan = planItem(materialId, context, displayName, lore, customModelData);
        if (plan.isVisualFallback()) {
            throw new IllegalArgumentException("Visual fallback requires createVisualItemWithFallback: " + materialId);
        }
        return create(plan);
    }

    public ItemStack createVisualItemWithFallback(String materialId, String displayName,
            List<String> lore, Integer customModelData) {
        return create(planItem(materialId, MaterialContext.GUI_ICON, displayName, lore, customModelData));
    }

    public LegacyGuiItemPlan planPlayerHead(String ownerName, String displayName, List<String> lore) {
        LegacyMaterialResolution skull = materials.resolveLegacyMaterial("SKULL_ITEM", MaterialContext.GUI_ICON);
        Material material = skull.getResolution().getMaterial().orElseThrow(
                new java.util.function.Supplier<IllegalStateException>() {
                    @Override public IllegalStateException get() {
                        return new IllegalStateException("Legacy SKULL_ITEM is unavailable");
                    }
                });
        String owner = ownerName == null || ownerName.trim().isEmpty() ? null : ownerName.trim();
        return new LegacyGuiItemPlan(material, (short) 3, text.format(displayName), formatLore(lore),
                false, false, owner);
    }

    public ItemStack createPlayerHead(String ownerName, String displayName, List<String> lore) {
        return create(planPlayerHead(ownerName, displayName, lore));
    }

    public boolean supportsCustomTextureHeads() {
        return false;
    }

    private ItemStack create(LegacyGuiItemPlan plan) {
        ItemStack item = new ItemStack(plan.getMaterial(), 1, plan.getData());
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        if (plan.getDisplayName() != null) meta.setDisplayName(plan.getDisplayName());
        if (!plan.getLore().isEmpty()) meta.setLore(plan.getLore());
        applySupportedFlags(meta);

        if (plan.getSkullOwner() != null) {
            if (!(meta instanceof SkullMeta)) {
                throw new IllegalStateException("SKULL_ITEM:3 did not provide SkullMeta");
            }
            ((SkullMeta) meta).setOwner(plan.getSkullOwner());
        }
        item.setItemMeta(meta);
        return item;
    }

    private List<String> formatLore(List<String> lore) {
        if (lore == null || lore.isEmpty()) return Collections.emptyList();
        List<String> result = new ArrayList<String>(lore.size());
        for (String line : lore) result.add(text.format(line));
        return result;
    }

    private static void applySupportedFlags(ItemMeta meta) {
        for (String name : VISUAL_FLAGS) {
            try {
                meta.addItemFlags(ItemFlag.valueOf(name));
            } catch (IllegalArgumentException ignored) {
                // Optional visual flag is absent on this runtime.
            }
        }
    }
}
