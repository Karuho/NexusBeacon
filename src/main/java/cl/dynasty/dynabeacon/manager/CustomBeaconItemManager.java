package cl.dynasty.dynabeacon.manager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import cl.dynasty.dynabeacon.DynaBeaconPlugin;
import cl.dynasty.dynabeacon.model.BeaconData;
import cl.dynasty.dynabeacon.util.ColorUtil;

public class CustomBeaconItemManager {

    private final DynaBeaconPlugin plugin;

    public CustomBeaconItemManager(DynaBeaconPlugin plugin) {
        this.plugin = plugin;
    }

    public ItemStack createBeaconItem(int amount) {
        ItemStack item = new ItemStack(Material.BEACON, amount);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ColorUtil.color("&bDynaBeacon"));
            meta.setLore(ColorUtil.color(Arrays.asList(
                    "&7Beacon avanzado de Dynacraft.",
                    "&7Colócalo para activar sus mejoras.")));
            item.setItemMeta(meta);
        }

        return plugin.getItemDataAdapter().writeBaseMarker(item);
    }

    public ItemStack createBeaconItemFromData(BeaconData beacon) {
        if (beacon.getEffectLevels().isEmpty()) {
            return createBeaconItem(1);
        }

        ItemStack item = createBeaconItem(1);
        ItemMeta meta = item.getItemMeta();

        if (meta == null)
            return item;

        List<String> lore = new ArrayList<String>();
        lore.add(ColorUtil.color("&7Beacon avanzado de Dynacraft."));
        lore.add(ColorUtil.color("&7Conserva sus efectos y niveles."));

        String ownerName = "Desconocido";

        if (beacon.getOwner() != null && org.bukkit.Bukkit.getOfflinePlayer(beacon.getOwner()).getName() != null) {
            ownerName = org.bukkit.Bukkit.getOfflinePlayer(beacon.getOwner()).getName();
        }

        lore.add(ColorUtil.color("&7Dueño: &f" + ownerName));
        if (beacon.getTrustedPlayers().isEmpty()) {
            lore.add(ColorUtil.color("&7Confiados: &cNinguno"));
        } else {
            lore.add(ColorUtil.color("&7Confiados: &a" + beacon.getTrustedPlayers().size()));
        }
        lore.add(ColorUtil.color("&8&m------------------------"));
        lore.add(ColorUtil.color("&bEfectos almacenados:"));

        for (Map.Entry<String, Integer> entry : beacon.getEffectLevels().entrySet()) {
            String effectName = entry.getKey();

            if (DynaBeaconPlugin.getInstance().getEffectRegistry().getEffect(entry.getKey()) != null) {
                effectName = DynaBeaconPlugin.getInstance()
                        .getEffectRegistry()
                        .getEffect(entry.getKey())
                        .getDisplayName();
            }

            lore.add(ColorUtil.color("&7- " + effectName + " &8| &dNivel " + entry.getValue()
                    + (beacon.getActiveEffects().contains(entry.getKey()) ? " &aActivo" : " &cInactivo")));
        }

        lore.add(ColorUtil.color("&8&m------------------------"));

        meta.setLore(lore);
        item.setItemMeta(meta);

        return plugin.getItemDataAdapter().writeBeaconData(item, beacon);
    }

    public boolean isCustomBeacon(ItemStack item) {
        if (plugin.getItemDataAdapter().isCustomBeacon(item)) {
            return true;
        }

        if (item == null || item.getType() != Material.BEACON || !item.hasItemMeta())
            return false;

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName())
            return false;

        String cleanName = ChatColor.stripColor(meta.getDisplayName());
        return cleanName != null && cleanName.equalsIgnoreCase("DynaBeacon");
    }

    public String readUniqueId(ItemStack item) {
        return plugin.getItemDataAdapter().readUniqueId(item);
    }

    public Map<String, Integer> readEffects(ItemStack item) {
        return plugin.getItemDataAdapter().readEffects(item);
    }

    public Set<String> readActiveEffects(ItemStack item) {
        return plugin.getItemDataAdapter().readActiveEffects(item);
    }
}