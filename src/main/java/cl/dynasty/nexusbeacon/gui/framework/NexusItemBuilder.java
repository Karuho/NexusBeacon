package cl.dynasty.nexusbeacon.gui.framework;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class NexusItemBuilder {

    private NexusItemBuilder() {
    }

    public static ItemStack build(Material material, String name, List<String> lore, NexusPlaceholderContext context) {
        ItemStack item = new ItemStack(material == null ? Material.STONE : material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            if (name != null) {
                meta.setDisplayName(color(context.apply(name)));
            }

            if (lore != null && !lore.isEmpty()) {
                List<String> coloredLore = new ArrayList<>();

                for (String line : lore) {
                    coloredLore.add(color(context.apply(line)));
                }

                meta.setLore(coloredLore);
            }

            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }

        return item;
    }

    public static ItemStack build(Material material, String name, List<String> lore) {
        return build(material, name, lore, new NexusPlaceholderContext());
    }

    public static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }
}