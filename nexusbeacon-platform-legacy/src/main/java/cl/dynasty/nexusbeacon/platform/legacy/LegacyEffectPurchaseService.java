package cl.dynasty.nexusbeacon.platform.legacy;

import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** Synchronous debit, durable state update, publication, and compensating refund transaction. */
public final class LegacyEffectPurchaseService {
    private final LegacyApplicationState state;
    private final LegacyEffectRuntime runtime;
    private final LegacyPaymentOptionResolver prices;
    private final LegacyEconomyService economy;
    private final Logger logger;

    public LegacyEffectPurchaseService(LegacyApplicationState state, LegacyEffectRuntime runtime,
            org.bukkit.configuration.file.FileConfiguration effects, LegacyMaterialResolver materials,
            LegacyEconomyService economy, Logger logger) {
        if (state == null || runtime == null || economy == null || logger == null) throw new NullPointerException();
        this.state = state;
        this.runtime = runtime;
        this.prices = new LegacyPaymentOptionResolver(effects, materials);
        this.economy = economy;
        this.logger = logger;
    }

    public synchronized LegacyPurchaseResult purchase(Player player, UUID beaconId, String effectId,
            String expectedAction, String optionKey) {
        if (player == null || beaconId == null || effectId == null || expectedAction == null || optionKey == null) {
            return LegacyPurchaseResult.STALE;
        }
        LegacyBeaconState current = state.findByUniqueId(beaconId);
        if (current == null) return LegacyPurchaseResult.REMOVED;
        UUID playerId = player.getUniqueId();
        if (!player.hasPermission("NexusBeacon.admin") && !playerId.equals(current.getOwner())
                && !current.getTrustedPlayers().contains(playerId)) return LegacyPurchaseResult.UNAUTHORIZED;
        LegacyEffectDefinition definition = runtime.getDefinition(effectId);
        if (definition == null || !definition.isSupported()) return LegacyPurchaseResult.UNSUPPORTED;
        Integer currentValue = current.getEffectLevels().get(definition.getId());
        int currentLevel = currentValue == null ? 0 : currentValue.intValue();
        String action = currentLevel == 0 ? "acquire" : "upgrade";
        if (!action.equalsIgnoreCase(expectedAction)) return LegacyPurchaseResult.STALE;
        int nextLevel = currentLevel + 1;
        if (nextLevel > definition.getMaxLevel() || !prices.isLevelEnabled(definition.getId(), nextLevel)) {
            return LegacyPurchaseResult.INVALID_LEVEL;
        }
        LegacyPaymentOption option = prices.resolve(definition.getId(), action, optionKey, nextLevel);
        if (option == null) return LegacyPurchaseResult.INVALID_PRICE;
        Debit debit = debit(player, option);
        if (debit.result != null) return debit.result;
        LegacyBeaconState replacement = current.withEffectLevel(definition.getId(), nextLevel, currentLevel == 0);
        try {
            if (!state.update(replacement)) {
                return compensate(player, debit, "State changed before durable purchase");
            }
        } catch (RuntimeException persistenceFailure) {
            logger.severe("Legacy effect " + action + " persistence failed for " + definition.getId()
                    + ": " + persistenceFailure.getMessage());
            return compensate(player, debit, "Persistence failed");
        }
        return LegacyPurchaseResult.COMMITTED;
    }

    private Debit debit(Player player, LegacyPaymentOption option) {
        int amount = option.getAmount();
        if (option.getType() == LegacyPaymentOption.Type.NONE) return new Debit(option, null);
        if (option.getType() == LegacyPaymentOption.Type.EXP_LEVEL) {
            if (player.getLevel() < amount) return new Debit(option, LegacyPurchaseResult.INSUFFICIENT_FUNDS);
            player.setLevel(player.getLevel() - amount);
            return new Debit(option, null);
        }
        if (option.getType() == LegacyPaymentOption.Type.ITEM) {
            ItemStack stack = new ItemStack(option.getMaterial(), amount);
            if (!player.getInventory().containsAtLeast(new ItemStack(option.getMaterial()), amount)) {
                return new Debit(option, LegacyPurchaseResult.INSUFFICIENT_FUNDS);
            }
            Map<Integer, ItemStack> remainder = player.getInventory().removeItem(stack);
            if (!remainder.isEmpty()) return new Debit(option, LegacyPurchaseResult.DEBIT_FAILED);
            player.updateInventory();
            return new Debit(option, null);
        }
        if (!economy.isAvailable()) return new Debit(option, LegacyPurchaseResult.ECONOMY_UNAVAILABLE);
        if (!economy.has(player, amount)) return new Debit(option, LegacyPurchaseResult.INSUFFICIENT_FUNDS);
        if (!economy.withdraw(player, amount)) return new Debit(option, LegacyPurchaseResult.DEBIT_FAILED);
        return new Debit(option, null);
    }

    private LegacyPurchaseResult compensate(Player player, Debit debit, String context) {
        boolean refunded;
        LegacyPaymentOption option = debit.option;
        if (option.getType() == LegacyPaymentOption.Type.NONE) refunded = true;
        else if (option.getType() == LegacyPaymentOption.Type.EXP_LEVEL) {
            player.setLevel(player.getLevel() + option.getAmount()); refunded = true;
        } else if (option.getType() == LegacyPaymentOption.Type.ITEM) {
            refunded = player.getInventory().addItem(new ItemStack(option.getMaterial(), option.getAmount())).isEmpty();
            player.updateInventory();
        } else refunded = economy.deposit(player, option.getAmount());
        if (refunded) return LegacyPurchaseResult.PERSISTENCE_FAILED_REFUNDED;
        logger.severe(context + "; compensation FAILED for player " + player.getUniqueId()
                + ", manual monetary reconciliation is required");
        return LegacyPurchaseResult.PERSISTENCE_FAILED_REFUND_FAILED;
    }

    private static final class Debit {
        private final LegacyPaymentOption option;
        private final LegacyPurchaseResult result;
        private Debit(LegacyPaymentOption option, LegacyPurchaseResult result) {
            this.option = option; this.result = result;
        }
    }
}
