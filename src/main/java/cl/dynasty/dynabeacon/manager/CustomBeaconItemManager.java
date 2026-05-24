package cl.dynasty.dynabeacon.manager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import cl.dynasty.dynabeacon.DynaBeaconPlugin;
import cl.dynasty.dynabeacon.effects.BeaconEffect;
import cl.dynasty.dynabeacon.model.BeaconData;
import cl.dynasty.dynabeacon.util.ColorUtil;

public class CustomBeaconItemManager {

    private final DynaBeaconPlugin plugin;

    public CustomBeaconItemManager(DynaBeaconPlugin plugin) {
        this.plugin = plugin;
    }

    public ItemStack createBeaconItem(int amount) {
        ItemStack item = createBaseItem(amount, null);
        return plugin.getItemDataAdapter().writeBaseMarker(item);
    }

    public ItemStack createBeaconItemFromData(BeaconData beacon) {
        ItemStack item = createBaseItem(1, beacon);

        if (beacon.getEffectLevels().isEmpty()) {
            return plugin.getItemDataAdapter().writeBaseMarker(item);
        }

        return plugin.getItemDataAdapter().writeBeaconData(item, beacon);
    }

    private ItemStack createBaseItem(int amount, BeaconData beacon) {
        Material material = plugin.getVersionAdapter().material(
                plugin.getConfigManager().getBeaconConfig().getString("item.material", "BEACON"));

        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return item;
        }

        String name = plugin.getConfigManager().getBeaconConfig()
                .getString("item.display-name", "&b&lDynaBeacon");

        List<String> loreTemplate = plugin.getConfigManager().getBeaconConfig()
                .getStringList("item.lore");

        if (loreTemplate.isEmpty()) {
            loreTemplate = new ArrayList<>();
            loreTemplate.add("&7Beacon avanzado de Dynasty.");
            loreTemplate.add("");
            loreTemplate.add("&8[&b+&8] &7Dueño: &f{owner}");
            loreTemplate.add("&8[&b+&8] &7Rango: &f{range}");
            loreTemplate.add("&8[&b+&8] &7Trust: &f{trusted}");
            loreTemplate.add("");
            loreTemplate.add("&8[&b+&8] &7Efectos:");
            loreTemplate.add("{effect_list}");
        }

        meta.setDisplayName(ColorUtil.color(applyPlaceholders(name, beacon)));

        List<String> parsedLore = new ArrayList<>();

        for (String line : loreTemplate) {
            if (line.equalsIgnoreCase("{effect_list}")) {
                parsedLore.addAll(buildEffectList(beacon));
                continue;
            }

            parsedLore.add(ColorUtil.color(applyPlaceholders(line, beacon)));
        }

        meta.setLore(parsedLore);
        item.setItemMeta(meta);

        return item;
    }

    private String applyPlaceholders(String text, BeaconData beacon) {
        if (text == null) {
            return "";
        }

        return text
                .replace("{owner}", getOwnerName(beacon))
                .replace("{range}", beacon != null ? String.valueOf(beacon.getRange()) : "0")
                .replace("{trusted}", getTrustedText(beacon));
    }

    private String getOwnerName(BeaconData beacon) {
        if (beacon == null || beacon.getOwner() == null) {
            return "Sin dueño";
        }

        OfflinePlayer owner = Bukkit.getOfflinePlayer(beacon.getOwner());
        return owner.getName() != null ? owner.getName() : beacon.getOwner().toString();
    }

    private String getTrustedText(BeaconData beacon) {
        if (beacon == null || beacon.getTrustedPlayers().isEmpty()) {
            return "No";
        }

        return String.valueOf(beacon.getTrustedPlayers().size());
    }

    private List<String> buildEffectList(BeaconData beacon) {
        List<String> lines = new ArrayList<>();

        if (beacon == null || beacon.getEffectLevels().isEmpty()) {
            lines.add(ColorUtil.color("&7- &cSin efectos adquiridos"));
            return lines;
        }

        for (Map.Entry<String, Integer> entry : beacon.getEffectLevels().entrySet()) {
            String effectId = entry.getKey();
            String effectName = effectId;

            BeaconEffect effect = plugin.getEffectRegistry().getEffect(effectId);
            if (effect != null) {
                effectName = effect.getDisplayName();
            }

            boolean active = beacon.getActiveEffects().contains(effectId);

            lines.add(ColorUtil.color("&7- " + effectName
                    + " &8| &dNivel " + entry.getValue()
                    + (active ? " &aActivo" : " &cInactivo")));
        }

        return lines;
    }

    public boolean isCustomBeacon(ItemStack item) {
        if (plugin.getItemDataAdapter().isCustomBeacon(item)) {
            return true;
        }

        if (item == null || item.getType() != Material.BEACON || !item.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();

        if (meta == null || !meta.hasDisplayName()) {
            return false;
        }

        String cleanName = ChatColor.stripColor(meta.getDisplayName());
        String configuredName = ChatColor.stripColor(ColorUtil.color(
                plugin.getConfigManager().getBeaconConfig().getString("item.display-name", "&b&lDynaBeacon")));

        return cleanName != null && cleanName.equalsIgnoreCase(configuredName);
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