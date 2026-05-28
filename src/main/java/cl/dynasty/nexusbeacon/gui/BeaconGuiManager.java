package cl.dynasty.nexusbeacon.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import cl.dynasty.nexusbeacon.NexusBeaconPlugin;
import cl.dynasty.nexusbeacon.effects.BeaconEffect;
import cl.dynasty.nexusbeacon.gui.framework.NexusGuiLoader;
import cl.dynasty.nexusbeacon.gui.framework.NexusGuiMenu;
import cl.dynasty.nexusbeacon.gui.framework.NexusPlaceholderContext;
import cl.dynasty.nexusbeacon.model.BeaconData;

public class BeaconGuiManager {

    private final NexusBeaconPlugin plugin;
    private final Map<UUID, String> openBeaconIds = new HashMap<>();
    private final Map<UUID, String> pendingEffectIds = new HashMap<>();
    private final Map<UUID, String> pendingActions = new HashMap<>();

    public BeaconGuiManager(NexusBeaconPlugin plugin) {
        this.plugin = plugin;
    }

    public String getOpenBeaconId(UUID uuid) {
        return openBeaconIds.get(uuid);
    }

    public BeaconData getOpenBeacon(Player player) {
        String id = openBeaconIds.get(player.getUniqueId());
        if (id == null)
            return null;
        return plugin.getBeaconManager().getBeaconById(id);
    }

    public void clear(Player player) {
        UUID uuid = player.getUniqueId();
        openBeaconIds.remove(uuid);
        pendingEffectIds.remove(uuid);
        pendingActions.remove(uuid);
    }

    private NexusPlaceholderContext createContext(Player player, BeaconData beacon) {
        return new NexusPlaceholderContext()
                .put("player", player.getName())
                .put("player_uuid", player.getUniqueId())
                .put("player_world", player.getWorld().getName())
                .put("player_level", player.getLevel())
                .put("player_exp", player.getExp())
                .put("player_health", Math.round(player.getHealth()))
                .put("player_max_health", Math.round(player.getMaxHealth()))
                .put("player_gamemode", player.getGameMode().name())
                .put("beacon_id", beacon.getId())
                .put("beacon_owner", Bukkit.getOfflinePlayer(beacon.getOwner()).getName())
                .put("active_effects", beacon.getActiveEffects().size())
                .put("beacon_world", beacon.getLocation().getWorld().getName())
                .put("beacon_x", beacon.getLocation().getBlockX())
                .put("beacon_y", beacon.getLocation().getBlockY())
                .put("beacon_z", beacon.getLocation().getBlockZ())
                .put("beacon_range", beacon.getRange())
                .put("beacon_location",
                        String.format("%s, %s, %s", beacon.getLocation().getBlockX(), beacon.getLocation().getBlockY(),
                                beacon.getLocation().getBlockZ()))
                .put("beacon_level", beacon.getLevel())
                .put("plugin_name", plugin.getName())
                .put("plugin_version", plugin.getDescription().getVersion())
                .put("server_name", plugin.getServer().getName())
                .put("server_version", plugin.getServer().getVersion())
                .put("range_particles_status", plugin.getLanguageManager().raw(
                        beacon.isRangeParticlesEnabled() ? "placeholders.enabled_female_plural"
                                : "placeholders.disabled_female_plural"))
                .put("range_particle_type", beacon.getRangeParticleType())
                .put("protect_base_status", plugin.getLanguageManager().raw(
                        beacon.isProtectBaseBlocks() ? "placeholders.enabled_female" : "placeholders.disabled_female"))
                .put("protect_base_blocks", plugin.getLanguageManager().raw(
                        beacon.isProtectBaseBlocks() ? "placeholders.yes" : "placeholders.no"))
                .put("beam_style",
                        beacon.getBeamStyle() != null ? beacon.getBeamStyle()
                                : plugin.getLanguageManager().raw("placeholders.global"))
                .put("trusted_players", beacon.getTrustedPlayers().size());
    }

    public void openMainMenu(Player player, BeaconData beacon) {
        openBeaconIds.put(player.getUniqueId(), beacon.getId());

        NexusPlaceholderContext context = createContext(player, beacon);

        NexusGuiLoader loader = new NexusGuiLoader(plugin);
        NexusGuiMenu menu = loader.loadMenu("main", context);

        if (menu != null) {
            menu.open(player);
        }
    }

    public void openEffectsMenu(Player player, BeaconData beacon) {
        openBeaconIds.put(player.getUniqueId(), beacon.getId());

        Inventory inventory = createGui(
                "effects",
                54,
                getGuiTitle("effects", plugin.getLanguageManager().raw("gui.effects.title")));

        int slot = 10;

        for (BeaconEffect effect : plugin.getEffectRegistry().getEffects()) {
            if (!cl.dynasty.nexusbeacon.effects.EffectLevelUtil.isLevelEnabled(plugin, effect, 1)) {
                continue;
            }
            boolean purchased = beacon.hasEffect(effect.getId());
            boolean active = beacon.isEffectActive(effect.getId());
            int level = beacon.getEffectLevel(effect.getId());

            int availablePower = plugin.getBeaconPowerManager().getAvailablePower(beacon);
            int usedPower = plugin.getBeaconPowerManager().getUsedPower(beacon);
            int displayLevel = Math.max(1, level);
            int effectPower = cl.dynasty.nexusbeacon.effects.EffectLevelUtil.getLevelInt(
                    plugin,
                    effect,
                    displayLevel,
                    "power-consumption",
                    effect.getPowerConsumption() * displayLevel);

            String rightClickText;

            if (!purchased) {
                rightClickText = plugin.getLanguageManager().raw("gui.effects.right-click-acquire");
            } else if (level >= effect.getMaxLevel()) {
                rightClickText = plugin.getLanguageManager().raw("gui.effects.maxed");
            } else {
                rightClickText = plugin.getLanguageManager().raw("gui.effects.right-click-upgrade");
            }

            inventory.setItem(slot, createItem(
                    effect.getIcon(),
                    effect.getDisplayName(),
                    effect.getDescription(),
                    purchased
                            ? (active
                                    ? plugin.getLanguageManager().raw("gui.effects.status-active")
                                    : plugin.getLanguageManager().raw("gui.effects.status-inactive"))
                            : plugin.getLanguageManager().raw("gui.effects.status-not-acquired"),
                    purchased
                            ? plugin.getLanguageManager().raw("gui.effects.level",
                                    Map.of(
                                            "level", String.valueOf(level),
                                            "max", String.valueOf(effect.getMaxLevel())))
                            : plugin.getLanguageManager().raw("gui.effects.level-not-acquired"),
                    plugin.getLanguageManager().raw("gui.effects.consumption",
                            Map.of("power", String.valueOf(effectPower))),
                    plugin.getLanguageManager().raw("gui.effects.beacon-power",
                            Map.of(
                                    "used", String.valueOf(usedPower),
                                    "available", String.valueOf(availablePower))),
                    "",
                    purchased
                            ? plugin.getLanguageManager().raw("gui.effects.left-click-toggle")
                            : plugin.getLanguageManager().raw("gui.effects.buy-from-payment"),
                    rightClickText));

            slot++;

            if (slot == 17)
                slot = 19;
            if (slot == 26)
                slot = 28;
            if (slot == 35)
                break;
        }

        inventory.setItem(49, createItem(
                Material.ARROW,
                plugin.getLanguageManager().raw("gui.effects.back-name"),
                plugin.getLanguageManager().raw("gui.effects.back-lore")));

        player.openInventory(inventory);
    }

    public void openSettingsMenu(Player player, BeaconData beacon) {
        openBeaconIds.put(player.getUniqueId(), beacon.getId());

        NexusPlaceholderContext context = createContext(player, beacon);

        NexusGuiLoader loader = new NexusGuiLoader(plugin);
        NexusGuiMenu menu = loader.loadMenu("settings", context);

        if (menu != null) {
            menu.open(player);
        }
    }

    public String getPendingEffectId(Player player) {
        return pendingEffectIds.get(player.getUniqueId());
    }

    public String getPendingAction(Player player) {
        return pendingActions.get(player.getUniqueId());
    }

    public void openPaymentMenu(Player player, BeaconData beacon, BeaconEffect effect, String action) {
        openBeaconIds.put(player.getUniqueId(), beacon.getId());
        pendingEffectIds.put(player.getUniqueId(), effect.getId());
        pendingActions.put(player.getUniqueId(), action);

        String title = action.equalsIgnoreCase("acquire")
                ? plugin.getLanguageManager().raw("gui.payment.title-acquire")
                : plugin.getLanguageManager().raw("gui.payment.title-upgrade");

        Inventory inventory = createGui(
                action.equalsIgnoreCase("acquire") ? "payment_acquire" : "payment_upgrade",
                27,
                getGuiTitle(action.equalsIgnoreCase("acquire") ? "payment-acquire" : "payment-upgrade", title));

        int nextLevel = beacon.getEffectLevel(effect.getId()) + 1;
        int diamondSlot = plugin.getConfigManager().getGuiConfig().getInt("legacy.payment.slots.diamond", 11);
        int expSlot = plugin.getConfigManager().getGuiConfig().getInt("legacy.payment.slots.exp", 13);
        int moneySlot = plugin.getConfigManager().getGuiConfig().getInt("legacy.payment.slots.money", 15);
        int backSlot = plugin.getConfigManager().getGuiConfig().getInt("legacy.payment.slots.back", 22);

        inventory.setItem(diamondSlot, createItem(
                Material.DIAMOND,
                plugin.getLanguageManager().raw("gui.payment.item-name"),
                plugin.getPaymentManager().getOptionText(effect, action, "diamond", nextLevel),
                "",
                plugin.getLanguageManager().raw("gui.payment.confirm")));

        inventory.setItem(expSlot, createItem(
                plugin.getVersionAdapter().material("EXPERIENCE_BOTTLE"),
                plugin.getLanguageManager().raw("gui.payment.exp-name"),
                plugin.getPaymentManager().getOptionText(effect, action, "exp", nextLevel),
                "",
                plugin.getLanguageManager().raw("gui.payment.confirm")));

        inventory.setItem(moneySlot, createItem(
                plugin.getVersionAdapter().material("SUNFLOWER"),
                plugin.getLanguageManager().raw("gui.payment.money-name"),
                plugin.getPaymentManager().getOptionText(effect, action, "money", nextLevel),
                "",
                plugin.getLanguageManager().raw("gui.payment.confirm")));

        inventory.setItem(backSlot, createItem(
                Material.ARROW,
                plugin.getLanguageManager().raw("gui.payment.back-name"),
                plugin.getLanguageManager().raw("gui.payment.back-lore")));

        player.openInventory(inventory);
    }

    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(plugin.getLanguageManager().color(name));

            List<String> coloredLore = Arrays.stream(lore)
                    .map(plugin.getLanguageManager()::color)
                    .toList();

            meta.setLore(coloredLore);

            meta.addItemFlags(
                    ItemFlag.HIDE_ATTRIBUTES,
                    ItemFlag.HIDE_ENCHANTS,
                    ItemFlag.HIDE_UNBREAKABLE,
                    ItemFlag.HIDE_ADDITIONAL_TOOLTIP);

            item.setItemMeta(meta);
        }

        return item;
    }

    private ItemStack createItem(Material material, String name, java.util.List<String> description,
            String... extraLore) {
        java.util.List<String> lore = new java.util.ArrayList<>();

        if (description != null) {
            lore.addAll(description);
        }

        lore.addAll(Arrays.asList(extraLore));

        return createItem(material, name, lore.toArray(new String[0]));
    }

    private ItemStack createPlayerHead(UUID uuid, String name, String... lore) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();

        if (meta != null) {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);

            meta.setOwningPlayer(offlinePlayer);
            meta.setDisplayName(plugin.getLanguageManager().color("&a" + name));

            List<String> coloredLore = new ArrayList<>();

            for (String line : lore) {
                coloredLore.add(plugin.getLanguageManager().color(line));
            }

            meta.setLore(coloredLore);
            item.setItemMeta(meta);
        }

        return item;
    }

    public void openTrustMenu(Player player, BeaconData beacon) {
        openBeaconIds.put(player.getUniqueId(), beacon.getId());

        NexusPlaceholderContext context = createContext(player, beacon);

        NexusGuiLoader loader = new NexusGuiLoader(plugin);
        NexusGuiMenu menu = loader.loadMenu("trust", context);

        if (menu != null) {
            menu.open(player);
        }
    }

    private Inventory createGui(String menuId, int size, String title) {
        NexusBeaconGuiHolder holder = new NexusBeaconGuiHolder(menuId);
        Inventory inventory = Bukkit.createInventory(holder, size, plugin.getLanguageManager().color(title));
        holder.setInventory(inventory);
        return inventory;
    }

    private String getGuiTitle(String key, String fallback) {
        return plugin.getConfigManager()
                .getGuiConfig()
                .getString("titles." + key, fallback);
    }
}