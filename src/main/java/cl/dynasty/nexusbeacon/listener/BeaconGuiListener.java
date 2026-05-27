package cl.dynasty.nexusbeacon.listener;

import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

import cl.dynasty.nexusbeacon.NexusBeaconPlugin;
import cl.dynasty.nexusbeacon.effects.BeaconEffect;
import cl.dynasty.nexusbeacon.gui.NexusBeaconGuiHolder;
import cl.dynasty.nexusbeacon.gui.framework.NexusGuiLoader;
import cl.dynasty.nexusbeacon.gui.framework.NexusGuiMenu;
import cl.dynasty.nexusbeacon.gui.framework.NexusPlaceholderContext;
import cl.dynasty.nexusbeacon.model.BeaconData;

public class BeaconGuiListener implements Listener {

    private final NexusBeaconPlugin plugin;

    public BeaconGuiListener(NexusBeaconPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player))
            return;

        if (!(event.getView().getTopInventory().getHolder() instanceof NexusBeaconGuiHolder)) {
            return;
        }

        NexusBeaconGuiHolder holder = (NexusBeaconGuiHolder) event.getView().getTopInventory().getHolder();
        String menuId = holder.getMenuId();

        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        BeaconData beacon = plugin.getBeaconGuiManager().getOpenBeacon(player);

        if (beacon == null) {
            player.closeInventory();
            return;
        }

        int slot = event.getRawSlot();

        if (menuId.equalsIgnoreCase("main")) {
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

        if (menuId.equalsIgnoreCase("settings")) {

            if (slot == 20) {
                beacon.setRangeParticlesEnabled(!beacon.isRangeParticlesEnabled());
                plugin.getStorageManager().saveBeacon(beacon);
                plugin.getBeaconGuiManager().openSettingsMenu(player, beacon);
                return;
            }

            if (slot == 22) {
                cycleParticle(beacon);
                plugin.getStorageManager().saveBeacon(beacon);
                plugin.getBeaconGuiManager().openSettingsMenu(player, beacon);
                return;
            }

            if (slot == 24) {
                beacon.setProtectBaseBlocks(!beacon.isProtectBaseBlocks());
                plugin.getStorageManager().saveBeacon(beacon);
                plugin.getBeaconGuiManager().openSettingsMenu(player, beacon);
                return;
            }

            if (slot == 30) {
                plugin.getBeaconGuiManager().openTrustMenu(player, beacon);
                return;
            }

            if (slot == 32) {
                NexusPlaceholderContext context = new NexusPlaceholderContext()
                        .put("beacon_id", beacon.getId());

                NexusGuiMenu menu = new NexusGuiLoader(plugin).loadMenu("beam-style", context);

                if (menu != null) {
                    menu.open(player);
                }

                return;
            }

            if (slot == 49) {
                plugin.getBeaconGuiManager().openMainMenu(player, beacon);
                return;
            }

            return;
        }

        if (menuId.equalsIgnoreCase("trust")) {
            if (slot == 49) {
                plugin.getBeaconGuiManager().openSettingsMenu(player, beacon);
                return;
            }

            if (slot == 22) {
                player.closeInventory();
                player.sendMessage(plugin.getLanguageManager().withPrefix("trust.use-command"));
                return;
            }

            int currentSlot = 10;

            for (UUID uuid : beacon.getTrustedPlayers()) {
                if (slot == currentSlot) {
                    beacon.removeTrusted(uuid);
                    plugin.getStorageManager().saveBeacon(beacon);
                    plugin.getBeaconGuiManager().openTrustMenu(player, beacon);
                    player.sendMessage(plugin.getLanguageManager().withPrefix("trust.removed"));
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

        if (menuId.equalsIgnoreCase("effects")) {
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

                if (!cl.dynasty.nexusbeacon.effects.EffectLevelUtil.isLevelEnabled(plugin, clickedEffect, nextLevel)) {
                    player.sendMessage(plugin.getLanguageManager().withPrefix("effect.disabled-level"));
                    return;
                }

                if (currentLevel >= clickedEffect.getMaxLevel()) {
                    player.sendMessage(plugin.getLanguageManager().withPrefix("effect.max-level"));
                    return;
                }

                String action = currentLevel <= 0 ? "acquire" : "upgrade";
                plugin.getBeaconGuiManager().openPaymentMenu(player, beacon, clickedEffect, action);
                return;
            }

            if (event.getClick() == ClickType.LEFT) {
                if (!beacon.hasEffect(clickedEffect.getId())) {
                    player.sendMessage(plugin.getLanguageManager().withPrefix("effect.must-acquire-first"));
                    return;
                }

                boolean newState = !beacon.isEffectActive(clickedEffect.getId());

                if (newState && !plugin.getBeaconPowerManager().canActivate(beacon, clickedEffect)) {
                    int available = plugin.getBeaconPowerManager().getAvailablePower(beacon);
                    int used = plugin.getBeaconPowerManager().getUsedPower(beacon);
                    player.sendMessage(plugin.getLanguageManager().withPrefix("effect.not-enough-power"));
                    player.sendMessage(plugin.getLanguageManager().get(
                            "effect.power-status",
                            java.util.Map.of(
                                    "used", String.valueOf(used),
                                    "available", String.valueOf(available))));
                    return;
                }

                beacon.setEffectActive(clickedEffect.getId(), newState);
                plugin.getStorageManager().saveBeacon(beacon);
                plugin.getBeaconGuiManager().openEffectsMenu(player, beacon);
                return;
            }
            return;
        }

        if (menuId.equalsIgnoreCase("payment_acquire") || menuId.equalsIgnoreCase("payment_upgrade")) {
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

            if (!cl.dynasty.nexusbeacon.effects.EffectLevelUtil.isLevelEnabled(plugin, effect, nextLevel)) {
                player.sendMessage(plugin.getLanguageManager().withPrefix("effect.disabled-level"));
                return;
            }

            if (!plugin.getPaymentManager().payOption(player, effect, action, optionKey, nextLevel)) {
                return;
            }

            if (action.equalsIgnoreCase("acquire")) {
                beacon.acquireEffect(effect.getId());
                player.sendMessage(plugin.getLanguageManager().withPrefix(
                        "effect.acquired",
                        java.util.Map.of("effect", effect.getDisplayName())));
            } else {
                beacon.setEffectLevel(effect.getId(), nextLevel);
                player.sendMessage(plugin.getLanguageManager().withPrefix(
                        "effect.upgraded",
                        java.util.Map.of(
                                "effect", effect.getDisplayName(),
                                "level", String.valueOf(nextLevel))));
            }

            plugin.getStorageManager().saveBeacon(beacon);
            plugin.getBeaconGuiManager().openEffectsMenu(player, beacon);
            return;
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof NexusBeaconGuiHolder) {
            event.setCancelled(true);
        }
    }

    private BeaconEffect getEffectBySlot(int slot) {
        int currentSlot = 10;

        for (BeaconEffect effect : plugin.getEffectRegistry().getEffects()) {
            if (!cl.dynasty.nexusbeacon.effects.EffectLevelUtil.isLevelEnabled(plugin, effect, 1)) {
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
}