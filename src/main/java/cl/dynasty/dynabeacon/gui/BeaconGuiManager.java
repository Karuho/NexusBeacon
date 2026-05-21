package cl.dynasty.dynabeacon.gui;

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

import cl.dynasty.dynabeacon.DynaBeaconPlugin;
import cl.dynasty.dynabeacon.effects.BeaconEffect;
import cl.dynasty.dynabeacon.model.BeaconData;
import cl.dynasty.dynabeacon.model.PlayerSettings;
import cl.dynasty.dynabeacon.util.ColorUtil;

public class BeaconGuiManager {

    private final DynaBeaconPlugin plugin;
    private final Map<UUID, String> openBeaconIds = new HashMap<>();
    private final Map<UUID, String> pendingEffectIds = new HashMap<>();
    private final Map<UUID, String> pendingActions = new HashMap<>();

    public BeaconGuiManager(DynaBeaconPlugin plugin) {
        this.plugin = plugin;
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

    public void openMainMenu(Player player, BeaconData beacon) {
        openBeaconIds.put(player.getUniqueId(), beacon.getId());

        Inventory inventory = Bukkit.createInventory(null, 54, ColorUtil.color("&8DynaBeacon"));

        inventory.setItem(20, createItem(Material.BEACON, "&bEfectos",
                "&7Efectos activos: &f" + beacon.getActiveEffects().size(),
                "&7Click para gestionar efectos."));

        inventory.setItem(24, createItem(
                plugin.getVersionAdapter().material("COMPARATOR", "REDSTONE_COMPARATOR"),
                "&eConfiguración",
                "&7Partículas, animaciones e idioma."));

        inventory.setItem(49, createItem(Material.BARRIER, "&cCerrar",
                "&7Cerrar este menú."));

        player.openInventory(inventory);
    }

    public void openEffectsMenu(Player player, BeaconData beacon) {
        openBeaconIds.put(player.getUniqueId(), beacon.getId());

        Inventory inventory = Bukkit.createInventory(null, 54, ColorUtil.color("&8DynaBeacon - Efectos"));

        int slot = 10;

        for (BeaconEffect effect : plugin.getEffectRegistry().getEffects()) {
            boolean purchased = beacon.hasEffect(effect.getId());
            boolean active = beacon.isEffectActive(effect.getId());
            int level = beacon.getEffectLevel(effect.getId());

            int availablePower = plugin.getBeaconPowerManager().getAvailablePower(beacon);
            int usedPower = plugin.getBeaconPowerManager().getUsedPower(beacon);
            int effectPower = effect.getPowerConsumption() * Math.max(1, level);

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

        Inventory inventory = Bukkit.createInventory(null, 54, ColorUtil.color("&8DynaBeacon - Config"));

        inventory.setItem(20, createItem(
                settings.isShowParticle() ? Material.EMERALD_BLOCK : Material.REDSTONE_BLOCK,
                "&bPartículas",
                settings.isShowParticle() ? "&7Estado: &aActivadas" : "&7Estado: &cDesactivadas",
                "&7Click para alternar."));

        inventory.setItem(22, createItem(
                Material.NETHER_STAR,
                "&dTipo de partícula",
                "&7Actual: &f" + settings.getParticleType(),
                "&7Click para cambiar."));

        inventory.setItem(24, createItem(
                beacon.isProtectBaseBlocks() ? Material.EMERALD_BLOCK : Material.REDSTONE_BLOCK,
                "&eProtección de base",
                beacon.isProtectBaseBlocks() ? "&7Estado: &aActivada" : "&7Estado: &cDesactivada",
                "&7Protege solo la pirámide válida.",
                "&7Click para alternar."));

        inventory.setItem(26, createItem(
                Material.PAPER,
                "&aJugadores confiables",
                "&7Jugadores confiados: &f" + beacon.getTrustedPlayers().size(),
                "&7Click para gestionar."));

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
                ? "&8DynaBeacon - Adquirir"
                : "&8DynaBeacon - Mejorar";

        Inventory inventory = Bukkit.createInventory(null, 27, ColorUtil.color(title));

        inventory.setItem(11, createItem(Material.DIAMOND, "&bPagar con ítem",
                plugin.getPaymentManager().getOptionText(effect, action, "diamond",
                        beacon.getEffectLevel(effect.getId()) + 1),
                "",
                "&eClick para confirmar."));

        inventory.setItem(13, createItem(Material.EXP_BOTTLE, "&aPagar con experiencia",
                plugin.getPaymentManager().getOptionText(effect, action, "exp",
                        beacon.getEffectLevel(effect.getId()) + 1),
                "",
                "&eClick para confirmar."));

        inventory.setItem(15, createItem(Material.DOUBLE_PLANT, "&6Pagar con dinero",
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
                    ItemFlag.HIDE_POTION_EFFECTS);

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

        Inventory inventory = Bukkit.createInventory(null, 54, ColorUtil.color("&8DynaBeacon - Trust"));

        int slot = 10;

        for (UUID uuid : beacon.getTrustedPlayers()) {
            OfflinePlayer trusted = Bukkit.getOfflinePlayer(uuid);
            String name = trusted.getName() != null ? trusted.getName() : uuid.toString();

            inventory.setItem(slot, createItem(
                    Material.SKULL_ITEM,
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
}