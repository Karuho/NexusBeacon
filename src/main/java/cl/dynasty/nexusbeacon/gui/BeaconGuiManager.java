package cl.dynasty.nexusbeacon.gui;

import java.util.Arrays;
import java.util.HashMap;
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

import cl.dynasty.nexusbeacon.NexusBeaconPlugin;
import cl.dynasty.nexusbeacon.effects.BeaconEffect;
import cl.dynasty.nexusbeacon.gui.framework.NexusGuiLoader;
import cl.dynasty.nexusbeacon.gui.framework.NexusGuiMenu;
import cl.dynasty.nexusbeacon.gui.framework.NexusPlaceholderContext;
import cl.dynasty.nexusbeacon.model.BeaconData;
import cl.dynasty.nexusbeacon.model.PlayerSettings;
import cl.dynasty.nexusbeacon.util.ColorUtil;

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
        openBeaconIds.remove(player.getUniqueId());
    }

    private NexusPlaceholderContext createContext(Player player, BeaconData beacon) {
        return new NexusPlaceholderContext()
                // Jugador
                .put("player", player.getName())
                .put("player_uuid", player.getUniqueId())
                .put("player_world", player.getWorld().getName())
                .put("player_level", player.getLevel())
                .put("player_exp", player.getExp())
                .put("player_health", Math.round(player.getHealth()))
                .put("player_max_health", Math.round(player.getMaxHealth()))
                .put("player_gamemode", player.getGameMode().name())

                // Beacon
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
                .put("protect_base_blocks", beacon.isProtectBaseBlocks() ? "Sí" : "No")
                .put("beam_style", beacon.getBeamStyle() != null ? beacon.getBeamStyle() : "global")

                // Plugin
                .put("plugin_name", plugin.getName())
                .put("plugin_version", plugin.getDescription().getVersion())
                .put("server_name", plugin.getServer().getName())
                .put("server_version", plugin.getServer().getVersion());
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

        Inventory inventory = createGui("effects", 54, getGuiTitle("effects", "&8NexusBeacon - Efectos"));

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
                rightClickText = "&aClick derecho: &fAdquirir";
            } else if (level >= effect.getMaxLevel()) {
                rightClickText = "&6¡Al máximo!";
            } else {
                rightClickText = "&bClick derecho: &fMejorar";
            }

            inventory.setItem(slot, createItem(
                    effect.getIcon(),
                    effect.getDisplayName(),
                    effect.getDescription(),
                    purchased ? (active ? "&7Estado: &aActivo" : "&7Estado: &cInactivo") : "&7Estado: &cNo adquirido",
                    purchased ? "&7Nivel: &f" + level + "&7/&f" + effect.getMaxLevel() : "&7Nivel: &cNo adquirido",
                    "&7Consumo: &e" + effectPower + " poder",
                    "&7Poder beacon: &f" + usedPower + "&7/&f" + availablePower,
                    "",
                    purchased ? "&eClick izquierdo: &fActivar/Desactivar"
                            : "&7Compra este efecto desde el menú de pago.",
                    rightClickText));

            slot++;

            if (slot == 17)
                slot = 19;
            if (slot == 26)
                slot = 28;
            if (slot == 35)
                break;
        }

        inventory.setItem(49, createItem(Material.ARROW, "&eVolver", "&7Volver al menú principal."));

        player.openInventory(inventory);
    }

    public void openSettingsMenu(Player player, BeaconData beacon) {
        openBeaconIds.put(player.getUniqueId(), beacon.getId());

        PlayerSettings settings = plugin.getPlayerSettingsManager().get(player.getUniqueId());

        Inventory inventory = createGui("settings", 54, getGuiTitle("settings", "&8NexusBeacon - Config"));

        inventory.setItem(20, createItem(
                beacon.isRangeParticlesEnabled() ? Material.EMERALD_BLOCK : Material.REDSTONE_BLOCK,
                "&bPartículas de rango",
                beacon.isRangeParticlesEnabled() ? "&7Estado: &aActivadas" : "&7Estado: &cDesactivadas",
                "&7Muestra la circunferencia del rango.",
                "&7Click para alternar."));

        inventory.setItem(22, createItem(
                Material.NETHER_STAR,
                "&dTipo de partícula de rango",
                "&7Actual: &f" + beacon.getRangeParticleType(),
                "&7Click para cambiar."));

        inventory.setItem(24, createItem(
                beacon.isProtectBaseBlocks() ? Material.REINFORCED_DEEPSLATE : Material.REDSTONE_BLOCK,
                "&eProtección de base",
                beacon.isProtectBaseBlocks() ? "&7Estado: &aActivada" : "&7Estado: &cDesactivada",
                "&7Protege solo la pirámide válida.",
                "&7Click para alternar."));

        inventory.setItem(30, createItem(
                Material.NAME_TAG,
                "&aJugadores confiables",
                "&7Jugadores confiados: &f" + beacon.getTrustedPlayers().size(),
                "&7Click para gestionar."));

        inventory.setItem(32, createItem(
                Material.LIGHT_BLUE_DYE,
                "&bRayo visual",
                "&7Estilo actual: &f" + (beacon.getBeamStyle() != null ? beacon.getBeamStyle() : "Global"),
                "&7Click para cambiar color/partícula."));

        inventory.setItem(49, createItem(Material.ARROW, "&eVolver", "&7Volver al menú principal."));

        player.openInventory(inventory);
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
                ? "&8NexusBeacon - Adquirir"
                : "&8NexusBeacon - Mejorar";

        Inventory inventory = createGui(action.equalsIgnoreCase("acquire") ? "payment_acquire" : "payment_upgrade",
                27,
                getGuiTitle(action.equalsIgnoreCase("acquire") ? "payment-acquire" : "payment-upgrade", title));

        inventory.setItem(11, createItem(Material.DIAMOND, "&bPagar con ítem",
                plugin.getPaymentManager().getOptionText(effect, action, "diamond",
                        beacon.getEffectLevel(effect.getId()) + 1),
                "",
                "&eClick para confirmar."));

        inventory.setItem(13,
                createItem(plugin.getVersionAdapter().material("EXPERIENCE_BOTTLE"), "&aPagar con experiencia",
                        plugin.getPaymentManager().getOptionText(effect, action, "exp",
                                beacon.getEffectLevel(effect.getId()) + 1),
                        "",
                        "&eClick para confirmar."));

        inventory.setItem(15, createItem(plugin.getVersionAdapter().material("SUNFLOWER"), "&6Pagar con dinero",
                plugin.getPaymentManager().getOptionText(effect, action, "money",
                        beacon.getEffectLevel(effect.getId()) + 1),
                "",
                "&eClick para confirmar."));

        inventory.setItem(22, createItem(Material.ARROW, "&eVolver", "&7Volver a efectos."));

        player.openInventory(inventory);
    }

    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ColorUtil.color(name));
            meta.setLore(ColorUtil.color(Arrays.asList(lore)));

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

    public void openTrustMenu(Player player, BeaconData beacon) {
        openBeaconIds.put(player.getUniqueId(), beacon.getId());

        Inventory inventory = createGui("trust", 54, getGuiTitle("trust", "&8NexusBeacon - Trust"));

        int slot = 10;

        for (UUID uuid : beacon.getTrustedPlayers()) {
            OfflinePlayer trusted = Bukkit.getOfflinePlayer(uuid);
            String name = trusted.getName() != null ? trusted.getName() : uuid.toString();

            inventory.setItem(slot, createItem(
                    plugin.getVersionAdapter().material("PLAYER_HEAD"),
                    "&a" + name,
                    "&7Jugador confiado.",
                    "&cClick para quitar trust."));

            slot++;

            if (slot == 17)
                slot = 19;
            if (slot == 26)
                slot = 28;
            if (slot == 35)
                break;
        }

        inventory.setItem(22, createItem(Material.PAPER, "&eAgregar jugador",
                "&7Usa el comando:",
                "&f/dbeacon trust <jugador>"));

        inventory.setItem(49, createItem(Material.ARROW, "&eVolver", "&7Volver a configuración."));

        player.openInventory(inventory);
    }

    private Inventory createGui(String menuId, int size, String title) {
        NexusBeaconGuiHolder holder = new NexusBeaconGuiHolder(menuId);
        Inventory inventory = Bukkit.createInventory(holder, size, ColorUtil.color(title));
        holder.setInventory(inventory);
        return inventory;
    }

    private String getGuiTitle(String key, String fallback) {
        return plugin.getConfigManager()
                .getGuiConfig()
                .getString("titles." + key, fallback);
    }
}