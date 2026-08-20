package cl.dynasty.nexusbeacon.platform.legacy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import cl.dynasty.nexusbeacon.platform.api.MaterialContext;

/** Productive holder-based Legacy menu renderer and session owner. */
public final class LegacyGuiController {
    private static final int[] CONTENT_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    private final Object identity = new Object();
    private final LegacyApplicationState state;
    private final LegacyInventoryFactory inventories;
    private final LegacyGuiItemFactory items;
    private final LegacyMessageService messages;
    private final LegacyEffectRuntime effects;
    private final LegacyGuiSessionRegistry sessions;
    private final LegacyGuiMutationService mutations;
    private final LegacyEffectPurchaseService purchases;
    private final LegacyPaymentOptionResolver prices;
    private final List<LegacyBeamStylePlan> beamStyles;

    public LegacyGuiController(LegacyApplicationGraph application, final LegacyEffectRuntime effects,
            org.bukkit.configuration.file.FileConfiguration effectsConfig, LegacyMaterialResolver materials,
            LegacyEconomyService economy, java.util.logging.Logger logger) {
        if (application == null || effects == null) throw new NullPointerException();
        this.state = application.getState();
        this.inventories = application.getInventories();
        this.items = application.getGuiItems();
        this.messages = application.getMessages();
        this.effects = effects;
        this.prices = new LegacyPaymentOptionResolver(effectsConfig, materials);
        this.purchases = new LegacyEffectPurchaseService(state, effects, effectsConfig, materials, economy, logger);
        this.sessions = new LegacyGuiSessionRegistry();
        this.beamStyles = LegacyBeamStylePlan.currentDefaults();
        this.mutations = new LegacyGuiMutationService(state, new LegacyGuiMutationService.RuntimeReadiness() {
            @Override public boolean isReady() { return effects.isRunning(); }
        }, beamStyles);
    }

    public boolean open(Player player, LegacyBeaconState beacon) {
        if (!isAuthorized(player, beacon)) {
            messages.sendChat(player, "\u00a7cYou are not trusted to configure this NexusBeacon.");
            return false;
        }
        open(player, beacon, LegacyGuiMenu.MAIN);
        return true;
    }

    void open(Player player, LegacyBeaconState beacon, LegacyGuiMenu menu) {
        LegacyGuiSession session = sessions.replace(player.getUniqueId(), beacon, menu);
        LegacyGuiHolder holder = new LegacyGuiHolder(identity, session);
        Inventory inventory = inventories.create(holder, menu == LegacyGuiMenu.PAYMENT ? 27 : 54, title(menu));
        holder.setInventory(inventory);
        if (menu == LegacyGuiMenu.MAIN) populateMain(inventory, beacon);
        else if (menu == LegacyGuiMenu.EFFECTS) populateEffects(inventory, beacon);
        else if (menu == LegacyGuiMenu.BEAM_STYLES) populateBeamStyles(inventory, beacon);
        player.openInventory(inventory);
    }

    LegacyGuiHolder holder(Inventory inventory) {
        if (inventory == null || !(inventory.getHolder() instanceof LegacyGuiHolder)) return null;
        LegacyGuiHolder holder = (LegacyGuiHolder) inventory.getHolder();
        return holder.belongsTo(identity) ? holder : null;
    }

    LegacyBeaconState currentAuthorized(Player player, LegacyGuiSession session) {
        return mutations.currentAuthorized(session, player.getUniqueId(), player.hasPermission("NexusBeacon.admin"));
    }

    LegacyGuiMutationResult toggleEffect(Player player, LegacyGuiSession session, String effectId) {
        return mutations.toggleEffect(session, player.getUniqueId(), player.hasPermission("NexusBeacon.admin"),
                effects.getDefinition(effectId));
    }

    LegacyGuiMutationResult selectBeamStyle(Player player, LegacyGuiSession session, String styleId) {
        return mutations.selectBeamStyle(session, player.getUniqueId(), player.hasPermission("NexusBeacon.admin"),
                styleId);
    }

    void openPayment(Player player, LegacyBeaconState beacon, String effectId, String action) {
        LegacyGuiSession session = sessions.replacePayment(player.getUniqueId(), beacon, effectId, action);
        LegacyGuiHolder holder = new LegacyGuiHolder(identity, session);
        Inventory inventory = inventories.create(holder, 27, title(LegacyGuiMenu.PAYMENT));
        holder.setInventory(inventory);
        int nextLevel = value(beacon.getEffectLevels().get(effectId)) + 1;
        inventory.setItem(11, paymentItem(effectId, action, "diamond", nextLevel, "DIAMOND", "Items"));
        inventory.setItem(13, paymentItem(effectId, action, "exp", nextLevel, "EXPERIENCE_BOTTLE", "EXP levels"));
        inventory.setItem(15, paymentItem(effectId, action, "money", nextLevel, "SUNFLOWER", "Vault money"));
        inventory.setItem(22, item("ARROW", "\u00a7eBack", "\u00a77Return to effects"));
        player.openInventory(inventory);
    }

    LegacyPurchaseResult purchase(Player player, LegacyGuiSession session, String optionKey) {
        if (!sessions.isCurrent(session)) return LegacyPurchaseResult.STALE;
        return purchases.purchase(player, session.getBeaconId(), session.getEffectId(),
                session.getPaymentAction(), optionKey);
    }

    String effectAt(int slot) {
        int index = indexOfSlot(slot);
        if (index < 0) return null;
        List<LegacyEffectDefinition> definitions = definitions();
        return index < definitions.size() ? definitions.get(index).getId() : null;
    }

    LegacyEffectDefinition effectDefinition(String effectId) { return effects.getDefinition(effectId); }

    String beamStyleAt(int slot) {
        int index = indexOfSlot(slot);
        return index >= 0 && index < beamStyles.size() ? beamStyles.get(index).getId() : null;
    }

    void close(LegacyGuiSession session) { sessions.removeIfCurrent(session); }
    void quit(UUID playerId) { sessions.remove(playerId); }
    public void close() { sessions.clear(); }
    public int getOpenSessionCount() { return sessions.size(); }

    private void populateMain(Inventory inventory, LegacyBeaconState beacon) {
        inventory.setItem(20, item("NETHER_STAR", "\u00a7bEffects",
                "\u00a77Select acquired effects", "\u00a77Active: " + beacon.getActiveEffects().size()));
        inventory.setItem(24, item("BEACON", "\u00a7bBeam styles",
                "\u00a77Current: " + value(beacon.getBeamStyle())));
        inventory.setItem(49, item("BARRIER", "\u00a7cClose", "\u00a77Close this menu"));
    }

    private void populateEffects(Inventory inventory, LegacyBeaconState beacon) {
        List<LegacyEffectDefinition> definitions = definitions();
        for (int index = 0; index < definitions.size() && index < CONTENT_SLOTS.length; index++) {
            LegacyEffectDefinition definition = definitions.get(index);
            boolean acquired = beacon.getEffectLevels().containsKey(definition.getId());
            boolean active = beacon.getActiveEffects().contains(definition.getId());
            String status = !definition.isSupported() ? "\u00a7cUnavailable: " + definition.getDiagnostic()
                    : !acquired ? "\u00a7eNot acquired"
                    : active ? "\u00a7aActive" : "\u00a77Inactive";
            inventory.setItem(CONTENT_SLOTS[index], item(definition.isSupported() ? "NETHER_STAR" : "BARRIER",
                    "\u00a7b" + definition.getId(), status,
                    !definition.isSupported() ? "\u00a77Fail closed"
                            : acquired && beacon.getEffectLevels().get(definition.getId()).intValue()
                                    >= definition.getMaxLevel() ? "\u00a77Left click to toggle; max level"
                            : acquired ? "\u00a77Left toggle; right upgrade"
                            : "\u00a77Right click to acquire"));
        }
        inventory.setItem(49, item("ARROW", "\u00a7eBack", "\u00a77Return to NexusBeacon"));
    }

    private void populateBeamStyles(Inventory inventory, LegacyBeaconState beacon) {
        for (int index = 0; index < beamStyles.size(); index++) {
            LegacyBeamStylePlan style = beamStyles.get(index);
            boolean selected = style.getId().equalsIgnoreCase(beacon.getBeamStyle());
            inventory.setItem(CONTENT_SLOTS[index], item("BEACON", "\u00a7b" + style.getId(),
                    selected ? "\u00a7aSelected" : "\u00a77Click to select"));
        }
        inventory.setItem(49, item("ARROW", "\u00a7eBack", "\u00a77Return to NexusBeacon"));
    }

    private ItemStack item(String material, String name, String... lore) {
        List<String> lines = new ArrayList<String>();
        Collections.addAll(lines, lore);
        return items.createItem(material, MaterialContext.GUI_ICON, name, lines, null);
    }

    private ItemStack paymentItem(String effectId, String action, String optionKey, int level,
            String icon, String label) {
        LegacyPaymentOption option = prices.resolve(effectId, action, optionKey, level);
        String cost = option == null ? "\u00a7cUnavailable / invalid configuration"
                : option.getType() == LegacyPaymentOption.Type.NONE ? "\u00a7aFree"
                : "\u00a77Cost: \u00a7f" + option.getAmount() + " " + label;
        return items.createVisualItemWithFallback(icon, "\u00a7b" + label,
                java.util.Arrays.asList(cost, "\u00a77Click to confirm"), null);
    }

    private List<LegacyEffectDefinition> definitions() {
        Collection<LegacyEffectDefinition> source = effects.getDefinitions().all();
        return new ArrayList<LegacyEffectDefinition>(source);
    }

    private static int indexOfSlot(int slot) {
        for (int index = 0; index < CONTENT_SLOTS.length; index++) if (CONTENT_SLOTS[index] == slot) return index;
        return -1;
    }

    private static String title(LegacyGuiMenu menu) {
        if (menu == LegacyGuiMenu.EFFECTS) return "\u00a78NexusBeacon Effects";
        if (menu == LegacyGuiMenu.PAYMENT) return "\u00a78NexusBeacon Payment";
        if (menu == LegacyGuiMenu.BEAM_STYLES) return "\u00a78NexusBeacon Beam";
        return "\u00a78NexusBeacon";
    }

    private static String value(String value) { return value == null ? "global" : value; }
    private static int value(Integer value) { return value == null ? 0 : value.intValue(); }

    private static boolean isAuthorized(Player player, LegacyBeaconState beacon) {
        UUID playerId = player.getUniqueId();
        return player.hasPermission("NexusBeacon.admin") || playerId.equals(beacon.getOwner())
                || beacon.getTrustedPlayers().contains(playerId);
    }
}
