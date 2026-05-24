package cl.dynasty.dynabeacon.listener;

import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

import cl.dynasty.dynabeacon.DynaBeaconPlugin;
import cl.dynasty.dynabeacon.effects.BeaconEffect;
import cl.dynasty.dynabeacon.model.BeaconData;
import cl.dynasty.dynabeacon.model.PlayerSettings;
import cl.dynasty.dynabeacon.util.ColorUtil;

public class BeaconGuiListener implements Listener {

    private final DynaBeaconPlugin plugin;

    public BeaconGuiListener(DynaBeaconPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player))
            return;

        String title = clean(event.getView().getTitle());

        if (!title.startsWith("DynaBeacon"))
            return;

        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        BeaconData beacon = plugin.getBeaconGuiManager().getOpenBeacon(player);

        if (beacon == null) {
            player.closeInventory();
            return;
        }

        int slot = event.getRawSlot();

        if (title.equalsIgnoreCase("DynaBeacon")) {
            if (slot == 20) {
                plugin.getBeaconGuiManager().openEffectsMenu(player, beacon);
                return;
            }

            if (slot == 24) {
                plugin.getBeaconGuiManager().openSettingsMenu(player, beacon);
                return;
            }

            if (slot == 49) {
                player.closeInventory();
            }

            return;
        }

        if (title.equalsIgnoreCase("DynaBeacon - Config")) {
            PlayerSettings settings = plugin.getPlayerSettingsManager().get(player.getUniqueId());

            if (slot == 20) {
                settings.setShowParticle(!settings.isShowParticle());
                plugin.getPlayerSettingsManager().save(settings);
                plugin.getBeaconGuiManager().openSettingsMenu(player, beacon);
                return;
            }

            if (slot == 22) {
                cycleParticle(settings);
                plugin.getPlayerSettingsManager().save(settings);
                plugin.getBeaconGuiManager().openSettingsMenu(player, beacon);
                return;
            }

            if (slot == 24) {
                beacon.setProtectBaseBlocks(!beacon.isProtectBaseBlocks());
                plugin.getStorageManager().saveBeacon(beacon);
                plugin.getBeaconGuiManager().openSettingsMenu(player, beacon);
                return;
            }

            if (slot == 26) {
                plugin.getBeaconGuiManager().openTrustMenu(player, beacon);
                return;
            }

            if (slot == 49) {
                plugin.getBeaconGuiManager().openMainMenu(player, beacon);
                return;
            }

            return;
        }

        if (title.equalsIgnoreCase("DynaBeacon - Trust")) {
            if (slot == 49) {
                plugin.getBeaconGuiManager().openSettingsMenu(player, beacon);
                return;
            }

            if (slot == 22) {
                player.closeInventory();
                player.sendMessage(ColorUtil.color("&b[DynaBeacon]&r &eUsa &f/dbeacon trust <jugador>&e."));
                return;
            }

            int currentSlot = 10;

            for (UUID uuid : beacon.getTrustedPlayers()) {
                if (slot == currentSlot) {
                    beacon.removeTrusted(uuid);
                    plugin.getStorageManager().saveBeacon(beacon);
                    plugin.getBeaconGuiManager().openTrustMenu(player, beacon);
                    player.sendMessage(ColorUtil.color("&b[DynaBeacon]&r &cJugador eliminado de trust."));
                    return;
                }

                currentSlot++;

                if (currentSlot == 17)
                    currentSlot = 19;
                if (currentSlot == 26)
                    currentSlot = 28;
                if (currentSlot == 35)
                    break;
            }

            return;
        }

        if (title.equalsIgnoreCase("DynaBeacon - Efectos")) {
            if (slot == 49) {
                plugin.getBeaconGuiManager().openMainMenu(player, beacon);
                return;
            }

            BeaconEffect clickedEffect = getEffectBySlot(slot);

            if (clickedEffect == null)
                return;

            if (event.getClick() == ClickType.RIGHT) {
                int currentLevel = beacon.getEffectLevel(clickedEffect.getId());
                int nextLevel = currentLevel + 1;

                if (!cl.dynasty.dynabeacon.effects.EffectLevelUtil.isLevelEnabled(plugin, clickedEffect, nextLevel)) {
                    player.sendMessage(ColorUtil.color("&b[DynaBeacon]&r &cEse nivel está desactivado."));
                    return;
                }

                if (currentLevel >= clickedEffect.getMaxLevel()) {
                    player.sendMessage(ColorUtil.color("&b[DynaBeacon]&r &eEste efecto ya está al nivel máximo."));
                    return;
                }

                String action = currentLevel <= 0 ? "acquire" : "upgrade";
                plugin.getBeaconGuiManager().openPaymentMenu(player, beacon, clickedEffect, action);
                return;
            }

            if (event.getClick() == ClickType.LEFT) {
                if (!beacon.hasEffect(clickedEffect.getId())) {
                    player.sendMessage(ColorUtil
                            .color("&b[DynaBeacon]&r &cPrimero debes adquirir este efecto con click derecho."));
                    return;
                }

                boolean newState = !beacon.isEffectActive(clickedEffect.getId());

                if (newState && !plugin.getBeaconPowerManager().canActivate(beacon, clickedEffect)) {
                    int available = plugin.getBeaconPowerManager().getAvailablePower(beacon);
                    int used = plugin.getBeaconPowerManager().getUsedPower(beacon);
                    player.sendMessage(ColorUtil.color("&b[DynaBeacon]&r &cNo hay poder suficiente."));
                    player.sendMessage(ColorUtil.color("&7Poder usado: &f" + used + "&7/&f" + available));
                    return;
                }

                beacon.setEffectActive(clickedEffect.getId(), newState);
                plugin.getStorageManager().saveBeacon(beacon);
                plugin.getBeaconGuiManager().openEffectsMenu(player, beacon);
                return;
            }
            return;
        }

        if (title.equalsIgnoreCase("DynaBeacon - Adquirir") || title.equalsIgnoreCase("DynaBeacon - Mejorar")) {
            if (slot == 22) {
                plugin.getBeaconGuiManager().openEffectsMenu(player, beacon);
                return;
            }

            String optionKey = null;

            if (slot == 11)
                optionKey = "diamond";
            if (slot == 13)
                optionKey = "exp";
            if (slot == 15)
                optionKey = "money";

            if (optionKey == null)
                return;

            String effectId = plugin.getBeaconGuiManager().getPendingEffectId(player);
            String action = plugin.getBeaconGuiManager().getPendingAction(player);

            BeaconEffect effect = plugin.getEffectRegistry().getEffect(effectId);

            if (effect == null || action == null) {
                player.closeInventory();
                return;
            }

            int currentLevel = beacon.getEffectLevel(effect.getId());
            int nextLevel = currentLevel + 1;

            if (!cl.dynasty.dynabeacon.effects.EffectLevelUtil.isLevelEnabled(plugin, effect, nextLevel)) {
                player.sendMessage(ColorUtil.color("&b[DynaBeacon]&r &cEse nivel está desactivado."));
                return;
            }

            if (!plugin.getPaymentManager().payOption(player, effect, action, optionKey, nextLevel)) {
                return;
            }

            if (action.equalsIgnoreCase("acquire")) {
                beacon.acquireEffect(effect.getId());
                player.sendMessage(ColorUtil.color("&b[DynaBeacon]&r &aEfecto adquirido: " + effect.getDisplayName()));
            } else {
                beacon.setEffectLevel(effect.getId(), nextLevel);
                player.sendMessage(ColorUtil.color(
                        "&b[DynaBeacon]&r &aEfecto mejorado: " + effect.getDisplayName() + " &7Nivel &f" + nextLevel));
            }

            plugin.getStorageManager().saveBeacon(beacon);
            plugin.getBeaconGuiManager().openEffectsMenu(player, beacon);
            return;
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (clean(event.getView().getTitle()).startsWith("DynaBeacon")) {
            event.setCancelled(true);
        }
    }

    private BeaconEffect getEffectBySlot(int slot) {
        int currentSlot = 10;

        for (BeaconEffect effect : plugin.getEffectRegistry().getEffects()) {
            if (!cl.dynasty.dynabeacon.effects.EffectLevelUtil.isLevelEnabled(plugin, effect, 1)) {
                continue;
            }

            if (slot == currentSlot) {
                return effect;
            }

            currentSlot++;

            if (currentSlot == 17)
                currentSlot = 19;
            if (currentSlot == 26)
                currentSlot = 28;
            if (currentSlot == 35)
                break;
        }

        return null;
    }

    private String clean(String title) {
        if (title == null)
            return "";
        return ChatColor.stripColor(title);
    }

    private void cycleParticle(PlayerSettings settings) {
        String current = settings.getParticleType();

        if (current.equalsIgnoreCase("VILLAGER_HAPPY")) {
            settings.setParticleType("FLAME");
            return;
        }

        if (current.equalsIgnoreCase("FLAME")) {
            settings.setParticleType("CRIT");
            return;
        }

        if (current.equalsIgnoreCase("CRIT")) {
            settings.setParticleType("CLOUD");
            return;
        }

        if (current.equalsIgnoreCase("CLOUD")) {
            settings.setParticleType("PORTAL");
            return;
        }

        settings.setParticleType("VILLAGER_HAPPY");
    }
}