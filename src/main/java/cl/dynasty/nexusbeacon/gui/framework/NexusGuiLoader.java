package cl.dynasty.nexusbeacon.gui.framework;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import cl.dynasty.nexusbeacon.NexusBeaconPlugin;
import cl.dynasty.nexusbeacon.effects.BeaconEffect;
import cl.dynasty.nexusbeacon.model.BeaconData;
import cl.dynasty.nexusbeacon.util.DebugLogger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;

public final class NexusGuiLoader {

    private final NexusBeaconPlugin plugin;

    public NexusGuiLoader(NexusBeaconPlugin plugin) {
        this.plugin = plugin;
    }

    public NexusGuiMenu loadMenu(String menuId, NexusPlaceholderContext context) {
        FileConfiguration guiConfig = plugin.getConfigManager().getGuiConfig();
        ConfigurationSection menuSection = guiConfig.getConfigurationSection("menus." + menuId);

        if (menuSection == null) {
            plugin.getLogger().warning(plugin.getLanguageManager().get(
                    "console.gui-menu-missing",
                    Map.of("menu", menuId)));
            return null;
        }

        String title = plugin.getLanguageManager().resolveLangValue(
                menuSection.getString("title", "&8NexusBeacon"));
        int size = resolveSize(menuSection);

        NexusGuiMenu menu = new NexusGuiMenu(menuId, title, size, context);

        DebugLogger.log(plugin, "gui-load:" + menuId,
                "Loaded GUI menu=" + menuId + " size=" + size);

        ConfigurationSection itemsSection = menuSection.getConfigurationSection("items");

        if (itemsSection == null) {
            return menu;
        }

        if (menuSection.isList("layout")) {
            loadLayoutItems(menu, menuSection, itemsSection, size, context);
        }

        loadDynamicItems(menu, menuSection, context);
        loadSlotItems(menu, itemsSection, size, context);

        return menu;
    }

    private void cycleParticle(BeaconData beacon) {
        String current = beacon.getRangeParticleType();

        if (current == null || current.isBlank()) {
            current = "VILLAGER_HAPPY";
        }

        if (current.equalsIgnoreCase("VILLAGER_HAPPY")) {
            beacon.setRangeParticleType("FLAME");
            return;
        }

        if (current.equalsIgnoreCase("FLAME")) {
            beacon.setRangeParticleType("CRIT");
            return;
        }

        if (current.equalsIgnoreCase("CRIT")) {
            beacon.setRangeParticleType("CLOUD");
            return;
        }

        if (current.equalsIgnoreCase("CLOUD")) {
            beacon.setRangeParticleType("PORTAL");
            return;
        }

        beacon.setRangeParticleType("VILLAGER_HAPPY");
    }

    private ItemStack createPlayerHead(UUID uuid, String name, List<String> lore, NexusPlaceholderContext context) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();

        if (meta != null) {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);

            meta.setOwningPlayer(offlinePlayer);
            meta.setDisplayName(plugin.getLanguageManager().color(context.apply("&a" + name)));

            List<String> coloredLore = lore.stream()
                    .map(context::apply)
                    .map(plugin.getLanguageManager()::color)
                    .toList();

            meta.setLore(coloredLore);
            item.setItemMeta(meta);
        }

        return item;
    }

    public NexusGuiMenu loadMenu(String menuId) {
        return loadMenu(menuId, new NexusPlaceholderContext());
    }

    private int resolveSize(ConfigurationSection menuSection) {
        if (menuSection.isList("layout")) {
            int rows = menuSection.getStringList("layout").size();
            return Math.max(9, Math.min(54, rows * 9));
        }

        return menuSection.getInt("size", 54);
    }

    private void loadLayoutItems(
            NexusGuiMenu menu,
            ConfigurationSection menuSection,
            ConfigurationSection itemsSection,
            int size,
            NexusPlaceholderContext context) {
        List<String> layout = menuSection.getStringList("layout");
        ConfigurationSection dynamicSection = menuSection.getConfigurationSection("dynamic");

        for (int row = 0; row < layout.size(); row++) {
            String line = layout.get(row);

            for (int column = 0; column < Math.min(9, line.length()); column++) {
                char symbol = line.charAt(column);

                if (symbol == '.' || symbol == ' ') {
                    continue;
                }

                String key = String.valueOf(symbol);

                if (dynamicSection != null && dynamicSection.contains(key)) {
                    continue;
                }

                ConfigurationSection itemSection = itemsSection.getConfigurationSection(key);

                if (itemSection == null) {
                    continue;
                }

                int slot = row * 9 + column;

                if (slot < 0 || slot >= size) {
                    continue;
                }

                menu.setItem(slot, createGuiItem(itemSection, context));
            }
        }
    }

    private void loadSlotItems(
            NexusGuiMenu menu,
            ConfigurationSection itemsSection,
            int size,
            NexusPlaceholderContext context) {
        for (String itemKey : itemsSection.getKeys(false)) {
            ConfigurationSection itemSection = itemsSection.getConfigurationSection(itemKey);

            if (itemSection == null || !itemSection.contains("slot")) {
                continue;
            }

            int slot = itemSection.getInt("slot", -1);

            if (slot < 0 || slot >= size) {
                plugin.getLogger().warning(plugin.getLanguageManager().get(
                        "console.gui-slot-invalid",
                        Map.of(
                                "menu", menu.getId(),
                                "item", itemKey)));
                continue;
            }

            menu.setItem(slot, createGuiItem(itemSection, context));
        }
    }

    private NexusGuiItem createGuiItem(ConfigurationSection itemSection, NexusPlaceholderContext context) {
        Material material = parseMaterial(itemSection.getString("material", "STONE"));
        String name = plugin.getLanguageManager().resolveLangValue(
                itemSection.getString("name", "&fItem"));

        List<String> lore = plugin.getLanguageManager().resolveLangList(
                itemSection.getStringList("lore"));
        String actionText = itemSection.getString("action", "");

        return new NexusGuiItem(
                NexusItemBuilder.build(material, name, lore, context),
                createAction(actionText));
    }

    private Material parseMaterial(String materialName) {
        if (materialName == null || materialName.isBlank()) {
            return Material.STONE;
        }

        Material material = Material.matchMaterial(materialName.toUpperCase());

        if (material == null) {
            plugin.getLogger().warning(plugin.getLanguageManager().get(
                    "console.gui-material-invalid",
                    Map.of("material", materialName)));
            return Material.STONE;
        }

        return material;
    }

    private NexusGuiAction createAction(String actionText) {
        if (actionText == null || actionText.isBlank()) {
            return null;
        }

        if (actionText.equalsIgnoreCase("close")) {
            return (player, menu, event) -> player.closeInventory();
        }

        if (actionText.equalsIgnoreCase("open:effects")) {
            return (player, menu, event) -> {
                BeaconData beacon = getOpenBeacon(player);

                if (beacon != null) {
                    plugin.getBeaconGuiManager().openEffectsMenu(player, beacon);
                }
            };
        }

        if (actionText.equalsIgnoreCase("open:settings")) {
            return (player, menu, event) -> {
                BeaconData beacon = getOpenBeacon(player);

                if (beacon != null) {
                    plugin.getBeaconGuiManager().openSettingsMenu(player, beacon);
                }
            };
        }

        if (actionText.startsWith("open:")) {
            String targetMenuId = actionText.substring("open:".length());

            return (player, menu, event) -> {
                NexusGuiMenu targetMenu = loadMenu(targetMenuId, menu.getContext());

                if (targetMenu != null) {
                    targetMenu.open(player);
                }
            };
        }

        if (actionText.startsWith("beam-style:")) {
            String styleId = actionText.substring("beam-style:".length());

            return (player, menu, event) -> {
                String beaconId = menu.getContext().apply("%beacon_id%");

                if (beaconId == null || beaconId.isBlank() || beaconId.equals("%beacon_id%")) {
                    player.closeInventory();
                    return;
                }

                BeaconData beacon = plugin.getBeaconManager().getBeacon(beaconId);

                if (beacon == null) {
                    player.closeInventory();
                    return;
                }

                beacon.setBeamStyle(styleId);
                plugin.getStorageManager().saveBeacon(beacon);

                String styleName = styleId;

                if (plugin.getBeamStyleManager().getStyle(styleId) != null) {
                    styleName = plugin.getBeamStyleManager().getStyle(styleId).getName();
                }

                player.sendMessage(plugin.getLanguageManager().withPrefix(
                        "beacon.beam-style-changed",
                        Map.of("style", styleName)));

                loadMenu("beam-style", menu.getContext()).open(player);
            };
        }

        if (actionText.equalsIgnoreCase("settings:toggle-range-particles")) {
            return (player, menu, event) -> {
                BeaconData beacon = getOpenBeacon(player);
                if (beacon == null)
                    return;

                beacon.setRangeParticlesEnabled(!beacon.isRangeParticlesEnabled());
                plugin.getStorageManager().saveBeacon(beacon);
                plugin.getBeaconGuiManager().openSettingsMenu(player, beacon);
            };
        }

        if (actionText.equalsIgnoreCase("settings:cycle-range-particle")) {
            return (player, menu, event) -> {
                BeaconData beacon = getOpenBeacon(player);
                if (beacon == null)
                    return;

                cycleParticle(beacon);
                plugin.getStorageManager().saveBeacon(beacon);
                plugin.getBeaconGuiManager().openSettingsMenu(player, beacon);
            };
        }

        if (actionText.equalsIgnoreCase("settings:toggle-base-protection")) {
            return (player, menu, event) -> {
                BeaconData beacon = getOpenBeacon(player);
                if (beacon == null)
                    return;

                beacon.setProtectBaseBlocks(!beacon.isProtectBaseBlocks());
                plugin.getStorageManager().saveBeacon(beacon);
                plugin.getBeaconGuiManager().openSettingsMenu(player, beacon);
            };
        }

        if (actionText.equalsIgnoreCase("trust:prepare-add")) {
            return (player, menu, event) -> {
                player.closeInventory();

                player.sendMessage(plugin.getLanguageManager().withPrefix("trust.prepare-add"));

                player.sendMessage(
                        Component.text(plugin.getLanguageManager().color(
                                plugin.getLanguageManager().raw("trust.prepare-add-click")))
                                .clickEvent(ClickEvent.suggestCommand("/nb trust ")));
            };
        }

        return (player, menu, event) -> plugin.getLogger().warning(plugin.getLanguageManager().get(
                "console.gui-action-unknown",
                Map.of("action", actionText)));
    }

    private BeaconData getOpenBeacon(Player player) {
        String beaconId = plugin.getBeaconGuiManager().getOpenBeaconId(player.getUniqueId());

        if (beaconId == null) {
            player.closeInventory();
            return null;
        }

        BeaconData beacon = plugin.getBeaconManager().getBeacon(beaconId);

        if (beacon == null) {
            player.closeInventory();
            return null;
        }

        return beacon;
    }

    private void loadDynamicItems(
            NexusGuiMenu menu,
            ConfigurationSection menuSection,
            NexusPlaceholderContext context) {
        ConfigurationSection dynamicSection = menuSection.getConfigurationSection("dynamic");

        if (dynamicSection == null || !menuSection.isList("layout")) {
            return;
        }

        List<String> layout = menuSection.getStringList("layout");

        for (String symbolKey : dynamicSection.getKeys(false)) {
            if (symbolKey.length() != 1) {
                plugin.getLogger().warning(plugin.getLanguageManager().get(
                        "console.gui-dynamic-symbol-invalid",
                        Map.of("symbol", symbolKey)));
                continue;
            }

            char symbol = symbolKey.charAt(0);
            String dynamicType = dynamicSection.getString(symbolKey, "");

            if (dynamicType.equalsIgnoreCase("effects")) {
                loadEffectItems(menu, layout, symbol, context);
            }

            if (dynamicType.equalsIgnoreCase("trusted")) {
                loadTrustedItems(menu, layout, symbol, context);
            }
        }
    }

    private void loadTrustedItems(
            NexusGuiMenu menu,
            List<String> layout,
            char symbol,
            NexusPlaceholderContext context) {
        List<Integer> slots = menu.getSlotsBySymbol(symbol, layout);

        String beaconId = context.apply("%beacon_id%");

        if (beaconId == null || beaconId.isBlank() || beaconId.equals("%beacon_id%")) {
            return;
        }

        BeaconData beacon = plugin.getBeaconManager().getBeacon(beaconId);

        if (beacon == null) {
            return;
        }

        int index = 0;

        for (java.util.UUID uuid : beacon.getTrustedPlayers()) {
            if (index >= slots.size()) {
                break;
            }

            org.bukkit.OfflinePlayer offlinePlayer = org.bukkit.Bukkit.getOfflinePlayer(uuid);
            String name = offlinePlayer.getName() != null ? offlinePlayer.getName() : uuid.toString();

            NexusPlaceholderContext trustedContext = new NexusPlaceholderContext()
                    .put("trusted_name", name)
                    .put("trusted_uuid", uuid.toString());

            int slot = slots.get(index);

            menu.setItem(slot, new NexusGuiItem(
                    createPlayerHead(
                            uuid,
                            "%trusted_name%",
                            plugin.getLanguageManager().rawList("gui.trust.trusted-player-lore", Map.of()),
                            trustedContext),
                    (player, clickedMenu, event) -> {
                        BeaconData clickedBeacon = getOpenBeacon(player);

                        if (clickedBeacon == null) {
                            return;
                        }

                        clickedBeacon.getTrustedPlayers().remove(uuid);
                        plugin.getStorageManager().saveBeacon(clickedBeacon);

                        player.sendMessage(plugin.getLanguageManager().withPrefix("trust.removed"));

                        NexusGuiMenu refreshed = loadMenu("trust", clickedMenu.getContext());

                        if (refreshed != null) {
                            refreshed.open(player);
                        }
                    }));

            index++;
        }
    }

    private void loadEffectItems(
            NexusGuiMenu menu,
            List<String> layout,
            char symbol,
            NexusPlaceholderContext context) {
        List<Integer> slots = menu.getSlotsBySymbol(symbol, layout);
        List<BeaconEffect> effects = new ArrayList<>(plugin.getEffectRegistry().getEffects());

        int index = 0;
        String beaconId = context.apply("%beacon_id%");
        BeaconData beacon = plugin.getBeaconManager().getBeacon(beaconId);

        for (BeaconEffect effect : effects) {
            if (index >= slots.size()) {
                break;
            }

            boolean active = beacon != null && beacon.getActiveEffects().contains(effect.getId());
            int level = beacon != null ? beacon.getEffectLevels().getOrDefault(effect.getId(), 1) : 1;

            NexusPlaceholderContext effectContext = new NexusPlaceholderContext()
                    .put("effect_id", effect.getId())
                    .put("effect_name", effect.getDisplayName())
                    .put("effect_type", effect.getType())
                    .put("effect_status", plugin.getLanguageManager().raw(
                            active ? "gui.effects.dynamic-status-active" : "gui.effects.dynamic-status-inactive"))
                    .put("effect_level", level)
                    .put("effect_max_level", effect.getMaxLevel())
                    .put("effect_power", effect.getPowerConsumption());

            int slot = slots.get(index);

            menu.setItem(slot, new NexusGuiItem(
                    NexusItemBuilder.build(
                            effect.getIcon(),
                            plugin.getLanguageManager().raw("gui.effects.dynamic-name"),
                            plugin.getLanguageManager().rawList("gui.effects.dynamic-lore", java.util.Map.of()),
                            effectContext),
                    (player, clickedMenu, event) -> {
                        String clickedBeaconId = plugin.getBeaconGuiManager().getOpenBeaconId(player.getUniqueId());

                        if (clickedBeaconId == null) {
                            player.closeInventory();
                            return;
                        }

                        BeaconData clickedBeacon = plugin.getBeaconManager().getBeacon(clickedBeaconId);

                        if (clickedBeacon == null) {
                            player.closeInventory();
                            return;
                        }

                        if (event.isRightClick()) {
                            int currentLevel = clickedBeacon.getEffectLevels().getOrDefault(effect.getId(), 1);

                            if (currentLevel >= effect.getMaxLevel()) {
                                player.sendMessage(plugin.getLanguageManager().withPrefix("effect.max-level"));
                                return;
                            }

                            clickedBeacon.getEffectLevels().put(effect.getId(), currentLevel + 1);
                            player.sendMessage(plugin.getLanguageManager().withPrefix(
                                    "effect.upgraded",
                                    Map.of(
                                            "effect", effect.getDisplayName(),
                                            "level", String.valueOf(currentLevel + 1))));
                        } else {
                            if (clickedBeacon.getActiveEffects().contains(effect.getId())) {
                                clickedBeacon.getActiveEffects().remove(effect.getId());
                                player.sendMessage(plugin.getLanguageManager().withPrefix(
                                        "effect.disabled",
                                        Map.of("effect", effect.getDisplayName())));
                            } else {
                                clickedBeacon.getActiveEffects().add(effect.getId());
                                clickedBeacon.getEffectLevels().putIfAbsent(effect.getId(), 1);
                                player.sendMessage(plugin.getLanguageManager().withPrefix(
                                        "effect.enabled",
                                        Map.of("effect", effect.getDisplayName())));
                            }
                        }

                        plugin.getStorageManager().saveBeacon(clickedBeacon);

                        NexusGuiMenu refreshed = loadMenu("effects", clickedMenu.getContext());

                        if (refreshed != null) {
                            refreshed.open(player);
                        }
                    }));

            index++;
        }
    }
}