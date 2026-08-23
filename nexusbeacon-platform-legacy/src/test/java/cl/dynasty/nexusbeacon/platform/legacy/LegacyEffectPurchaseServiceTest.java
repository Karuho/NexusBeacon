package cl.dynasty.nexusbeacon.platform.legacy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import cl.dynasty.nexusbeacon.platform.api.ScheduledTaskHandle;
import cl.dynasty.nexusbeacon.platform.api.SchedulerService;

class LegacyEffectPurchaseServiceTest {
    @Test void purchaseAndUpgradeDebitExactlyAndPublishDurably() {
        Fixture fixture = new Fixture();
        assertEquals(LegacyPurchaseResult.COMMITTED, fixture.acquire());
        assertEquals(9750D, fixture.economy.balance);
        assertEquals(1, fixture.level());
        assertTrue(fixture.active());
        assertEquals(LegacyPurchaseResult.COMMITTED, fixture.upgrade());
        assertEquals(9150D, fixture.economy.balance);
        assertEquals(2, fixture.level());
    }

    @Test void duplicateAndStaleClicksNeverDoubleChargeOrSkipLevel() {
        Fixture fixture = new Fixture();
        assertEquals(LegacyPurchaseResult.COMMITTED, fixture.acquire());
        assertEquals(LegacyPurchaseResult.STALE, fixture.acquire());
        assertEquals(9750D, fixture.economy.balance);
        assertEquals(1, fixture.level());
    }

    @Test void unauthorizedAndRemovedBeaconFailBeforeDebit() {
        Fixture fixture = new Fixture();
        assertEquals(LegacyPurchaseResult.UNAUTHORIZED, fixture.service.purchase(
                fixture.player(UUID.randomUUID(), false), fixture.beaconId, "furnace_boost", "acquire", "money"));
        fixture.state.delete(fixture.location);
        assertEquals(LegacyPurchaseResult.REMOVED, fixture.acquire());
        assertEquals(10000D, fixture.economy.balance);
    }

    @Test void trustedPlayerUsesSameCurrentAuthorizationSemantics() {
        Fixture fixture = new Fixture();
        assertEquals(LegacyPurchaseResult.COMMITTED, fixture.service.purchase(
                fixture.player(fixture.trusted, false), fixture.beaconId, "furnace_boost", "acquire", "money"));
    }

    @Test void unavailableInsufficientAndWithdrawFailureNeverPublish() {
        Fixture unavailable = new Fixture(); unavailable.economy.available = false;
        assertEquals(LegacyPurchaseResult.ECONOMY_UNAVAILABLE, unavailable.acquire());
        Fixture poor = new Fixture(); poor.economy.balance = 1;
        assertEquals(LegacyPurchaseResult.INSUFFICIENT_FUNDS, poor.acquire());
        Fixture failed = new Fixture(); failed.economy.withdraw = false;
        assertEquals(LegacyPurchaseResult.DEBIT_FAILED, failed.acquire());
        assertEquals(0, unavailable.level()); assertEquals(0, poor.level()); assertEquals(0, failed.level());
    }

    @Test void invalidMissingAndNonPositivePricesFailClosed() {
        Fixture missing = new Fixture(); missing.effects.set("effects.furnace_boost.levels.1.costs.acquire.options.money", null);
        missing.rebuild();
        assertEquals(LegacyPurchaseResult.INVALID_PRICE, missing.acquire());
        Fixture zero = new Fixture(); zero.effects.set("effects.furnace_boost.levels.1.costs.acquire.options.money.amount", 0);
        zero.rebuild();
        assertEquals(LegacyPurchaseResult.INVALID_PRICE, zero.acquire());
        assertEquals(10000D, zero.economy.balance);
    }

    @Test void maximumAndDisabledNextLevelRejectWithoutCharge() {
        Fixture fixture = new Fixture(); fixture.acquire(); fixture.upgrade();
        assertEquals(LegacyPurchaseResult.INVALID_LEVEL, fixture.upgrade());
        assertEquals(9150D, fixture.economy.balance);
    }

    @Test void globalAmountPerLevelUpgradeMatchesModernWhenLevelSectionsAreAbsent() {
        FileConfiguration config = new YamlConfiguration();
        config.set("effects.speed.costs.upgrade.options.money.type", "VAULT_MONEY");
        config.set("effects.speed.costs.upgrade.options.money.amount-per-level", 6000);
        LegacyPaymentOptionResolver resolver = new LegacyPaymentOptionResolver(config, new LegacyMaterialResolver());
        assertTrue(resolver.isLevelEnabled("speed", 2));
        assertEquals(12000, resolver.resolve("speed", "upgrade", "money", 2).getAmount());
    }

    @Test void persistenceFailureRefundsAndNeverPublishes() {
        Fixture fixture = new Fixture(); fixture.storage.failNext = true;
        assertEquals(LegacyPurchaseResult.PERSISTENCE_FAILED_REFUNDED, fixture.acquire());
        assertEquals(10000D, fixture.economy.balance);
        assertEquals(0, fixture.level());
    }

    @Test void refundFailureIsExplicitAndStillNeverPublishes() {
        Fixture fixture = new Fixture(); fixture.storage.failNext = true; fixture.economy.deposit = false;
        assertEquals(LegacyPurchaseResult.PERSISTENCE_FAILED_REFUND_FAILED, fixture.acquire());
        assertEquals(9750D, fixture.economy.balance);
        assertEquals(0, fixture.level());
    }

    private static final class Fixture {
        private final UUID owner = UUID.randomUUID();
        private final UUID trusted = UUID.randomUUID();
        private final UUID beaconId = UUID.randomUUID();
        private final LegacyBeaconLocation location = new LegacyBeaconLocation("world", 0, 64, 0);
        private final Storage storage = new Storage();
        private final LegacyApplicationState state = new LegacyApplicationState(storage);
        private final Economy economy = new Economy();
        private final FileConfiguration effects = new YamlConfiguration();
        private LegacyEffectRuntime runtime;
        private LegacyEffectPurchaseService service;

        private Fixture() {
            storage.values.add(new LegacyBeaconState(location, beaconId, owner, 48, 1,
                    Collections.<String, Integer>emptyMap(), Collections.<String>emptySet(),
                    Collections.singleton(trusted), true, "aqua", true, "VILLAGER_HAPPY"));
            state.initialize();
            effects.set("effects.furnace_boost.enabled", true);
            effects.set("effects.furnace_boost.type", "BLOCK_PROCESS_BOOST");
            effects.set("effects.furnace_boost.target-blocks", Collections.singletonList("FURNACE"));
            effects.set("effects.furnace_boost.max-level", 2);
            effects.set("effects.furnace_boost.levels.1.enabled", true);
            effects.set("effects.furnace_boost.levels.1.speed-up-time", 8);
            effects.set("effects.furnace_boost.levels.1.costs.acquire.options.money.type", "VAULT_MONEY");
            effects.set("effects.furnace_boost.levels.1.costs.acquire.options.money.amount", 250);
            effects.set("effects.furnace_boost.levels.2.enabled", true);
            effects.set("effects.furnace_boost.levels.2.speed-up-time", 16);
            effects.set("effects.furnace_boost.levels.2.costs.upgrade.options.money.type", "VAULT_MONEY");
            effects.set("effects.furnace_boost.levels.2.costs.upgrade.options.money.amount", 600);
            rebuild();
        }

        private void rebuild() {
            FileConfiguration beacon = new YamlConfiguration(); beacon.set("beacon.tick-interval", 40);
            Plugin plugin = (Plugin) Proxy.newProxyInstance(Plugin.class.getClassLoader(),
                    new Class<?>[] { Plugin.class }, (proxy, method, args) -> null);
            runtime = new LegacyEffectRuntime(plugin, state, beacon, effects, new LegacyMaterialResolver(),
                    new LegacyPotionEffectResolver(), new Scheduler());
            service = new LegacyEffectPurchaseService(state, runtime, effects, new LegacyMaterialResolver(), economy,
                    Logger.getLogger("purchase-test"));
        }
        private LegacyPurchaseResult acquire() { return service.purchase(player(owner, false), beaconId,
                "furnace_boost", "acquire", "money"); }
        private LegacyPurchaseResult upgrade() { return service.purchase(player(owner, false), beaconId,
                "furnace_boost", "upgrade", "money"); }
        private int level() { Integer value = state.find(location).getEffectLevels().get("furnace_boost");
            return value == null ? 0 : value.intValue(); }
        private boolean active() { return state.find(location).getActiveEffects().contains("furnace_boost"); }
        private Player player(final UUID id, final boolean admin) {
            return (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[] { Player.class },
                    (proxy, method, args) -> "getUniqueId".equals(method.getName()) ? id
                            : "hasPermission".equals(method.getName()) ? Boolean.valueOf(admin)
                            : primitive(method.getReturnType()));
        }
    }

    private static final class Economy implements LegacyEconomyService {
        private boolean available = true, withdraw = true, deposit = true;
        private double balance = 10000;
        @Override public boolean isAvailable() { return available; }
        @Override public boolean has(Player player, double amount) { return balance >= amount; }
        @Override public boolean withdraw(Player player, double amount) { if (!withdraw) return false; balance -= amount; return true; }
        @Override public boolean deposit(Player player, double amount) { if (!deposit) return false; balance += amount; return true; }
        @Override public String getDiagnostic() { return available ? "available" : "unavailable"; }
    }

    private static final class Storage implements LegacyBeaconStorage {
        private final List<LegacyBeaconState> values = new ArrayList<LegacyBeaconState>();
        private boolean failNext;
        @Override public LegacyStorageLoadResult load() { return LegacyStorageLoadResult.success(values); }
        @Override public void store(Collection<LegacyBeaconState> beacons) {
            if (failNext) { failNext = false; throw new LegacyStorageException("injected"); }
            values.clear(); values.addAll(beacons);
        }
        @Override public void close() { }
        @Override public String getBackendName() { return "TEST"; }
    }

    private static final class Scheduler implements SchedulerService {
        @Override public void runSync(Runnable runnable) { runnable.run(); }
        @Override public void runSync(Location location, Runnable runnable) { runnable.run(); }
        @Override public void runSync(Entity entity, Runnable runnable) { runnable.run(); }
        @Override public ScheduledTaskHandle runSyncLater(Runnable runnable, long delayTicks) { return handle(); }
        @Override public ScheduledTaskHandle runSyncTimer(Runnable runnable, long delayTicks, long period) { return handle(); }
        @Override public void runAsync(Runnable runnable) { }
        @Override public ScheduledTaskHandle runAsyncLater(Runnable runnable, long delayTicks) { return handle(); }
        @Override public ScheduledTaskHandle runAsyncTimer(Runnable runnable, long delayTicks, long period) { return handle(); }
        private ScheduledTaskHandle handle() { return new ScheduledTaskHandle() { @Override public void cancel() { } }; }
    }

    private static Object primitive(Class<?> type) {
        if (type == Boolean.TYPE) return Boolean.FALSE;
        if (type == Integer.TYPE) return Integer.valueOf(0);
        if (type == Long.TYPE) return Long.valueOf(0L);
        if (type == Double.TYPE) return Double.valueOf(0D);
        if (type == Float.TYPE) return Float.valueOf(0F);
        if (type == Short.TYPE) return Short.valueOf((short) 0);
        if (type == Byte.TYPE) return Byte.valueOf((byte) 0);
        return null;
    }
}
