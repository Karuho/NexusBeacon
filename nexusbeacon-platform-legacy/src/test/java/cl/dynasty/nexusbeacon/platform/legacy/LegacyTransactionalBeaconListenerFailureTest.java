package cl.dynasty.nexusbeacon.platform.legacy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;

class LegacyTransactionalBeaconListenerFailureTest {
    @Test void saveFailureCancelsProvisionalPlacementWithoutSuccessSideEffect() {
        Fixture fixture = new Fixture();
        fixture.storage.failWrites = true;
        AtomicReference<String> sent = new AtomicReference<String>();
        Player player = player(GameMode.SURVIVAL, sent);
        ItemStack marked = new ItemStack(Material.BEACON);
        fixture.bridge.status.put(marked, LegacyIdentityStatus.RECOGNIZED);
        Block block = block(Material.BEACON, new AtomicInteger());
        BlockPlaceEvent event = new BlockPlaceEvent(block, state(Material.AIR), block,
                marked, player, true);

        fixture.listener().onBeaconPlace(event);

        assertTrue(event.isCancelled());
        assertEquals(0, fixture.state.size());
        assertEquals("\u00a7cfailed", sent.get());
    }

    @Test void deleteFailureKeepsBlockAndStateAndIssuesNoReturnOrSuccess() {
        Fixture fixture = new Fixture();
        LegacyBeaconState beacon = fixture.beacon();
        fixture.state.insert(beacon);
        fixture.storage.failWrites = true;
        AtomicReference<String> sent = new AtomicReference<String>();
        AtomicInteger blockMutations = new AtomicInteger();
        Block block = block(Material.BEACON, blockMutations);
        BlockBreakEvent event = new BlockBreakEvent(block, player(GameMode.CREATIVE, sent));

        fixture.listener().onBeaconBreak(event);

        assertTrue(event.isCancelled());
        assertSame(beacon, fixture.state.find(beacon.getLocation()));
        assertEquals(0, blockMutations.get());
        assertEquals("\u00a7cfailed", sent.get());
    }

    private static final class Fixture {
        private final RecordingStorage storage = new RecordingStorage();
        private final LegacyApplicationState state = new LegacyApplicationState(storage);
        private final FakeBridge bridge = new FakeBridge();
        private final LegacyItemIdentityService identities = new LegacyItemIdentityService(bridge);
        private final LegacyBeaconGameplaySettings settings = new LegacyBeaconGameplaySettings(
                48, true, true, "VILLAGER_HAPPY", true, true, true, true);

        private Fixture() { state.initialize(); }

        private LegacyTransactionalBeaconListener listener() {
            return new LegacyTransactionalBeaconListener(plugin(), state, identities,
                    new LegacyBeaconTransactionService(state, identities, settings), null, settings,
                    new LegacyMessageService(new LegacyTextFormatter()),
                    new LegacyBeaconListenerMessages("&aplaced", "&aremoved", "&cpermission",
                            "&cowner", "&cfull", "&cfailed", "&cduplicate", "&cinvalid"),
                    Material.BEACON);
        }

        private LegacyBeaconState beacon() {
            return new LegacyBeaconState(new LegacyBeaconLocation("world", 1, 64, 2),
                    UUID.fromString("11111111-1111-4111-8111-111111111111"),
                    UUID.fromString("22222222-2222-4222-8222-222222222222"), 48, 1,
                    Collections.<String, Integer>emptyMap(), Collections.<String>emptySet(),
                    Collections.<UUID>emptySet(), true, null, true, "VILLAGER_HAPPY");
        }
    }

    private static Player player(GameMode mode, AtomicReference<String> sent) {
        return (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[] { Player.class },
                (instance, method, args) -> {
                    if (method.getName().equals("getUniqueId")) {
                        return UUID.fromString("22222222-2222-4222-8222-222222222222");
                    }
                    if (method.getName().equals("getGameMode")) return mode;
                    if (method.getName().equals("hasPermission")) return Boolean.TRUE;
                    if (method.getName().equals("sendMessage") && args.length == 1) {
                        sent.set((String) args[0]);
                    }
                    return defaultValue(method.getReturnType());
                });
    }

    private static Block block(Material material, AtomicInteger mutations) {
        World world = (World) Proxy.newProxyInstance(World.class.getClassLoader(), new Class<?>[] { World.class },
                (instance, method, args) -> method.getName().equals("getName")
                        ? "world" : defaultValue(method.getReturnType()));
        return (Block) Proxy.newProxyInstance(Block.class.getClassLoader(), new Class<?>[] { Block.class },
                (instance, method, args) -> {
                    if (method.getName().equals("getType")) return material;
                    if (method.getName().equals("getWorld")) return world;
                    if (method.getName().equals("getX")) return Integer.valueOf(1);
                    if (method.getName().equals("getY")) return Integer.valueOf(64);
                    if (method.getName().equals("getZ")) return Integer.valueOf(2);
                    if (method.getName().equals("setType")) mutations.incrementAndGet();
                    return defaultValue(method.getReturnType());
                });
    }

    private static BlockState state(Material material) {
        return (BlockState) Proxy.newProxyInstance(BlockState.class.getClassLoader(),
                new Class<?>[] { BlockState.class }, (instance, method, args) ->
                        method.getName().equals("getType") ? material : defaultValue(method.getReturnType()));
    }

    private static Plugin plugin() {
        return (Plugin) Proxy.newProxyInstance(Plugin.class.getClassLoader(), new Class<?>[] { Plugin.class },
                (instance, method, args) -> defaultValue(method.getReturnType()));
    }

    private static Object defaultValue(Class<?> type) {
        if (type == boolean.class) return Boolean.FALSE;
        if (type == byte.class) return Byte.valueOf((byte) 0);
        if (type == short.class) return Short.valueOf((short) 0);
        if (type == int.class) return Integer.valueOf(0);
        if (type == long.class) return Long.valueOf(0L);
        if (type == float.class) return Float.valueOf(0F);
        if (type == double.class) return Double.valueOf(0D);
        if (type == char.class) return Character.valueOf('\0');
        return null;
    }

    private static final class FakeBridge implements LegacyNbtBridge {
        private final Map<ItemStack, LegacyIdentityStatus> status =
                new IdentityHashMap<ItemStack, LegacyIdentityStatus>();
        @Override public ItemStack mark(ItemStack item) { return item; }
        @Override public LegacyIdentityStatus identify(ItemStack item) {
            LegacyIdentityStatus value = status.get(item);
            return value == null ? LegacyIdentityStatus.NOT_RECOGNIZED : value;
        }
        @Override public String getRevision() { return "test"; }
    }

    private static final class RecordingStorage implements LegacyBeaconStorage {
        private boolean failWrites;
        @Override public LegacyStorageLoadResult load() {
            return LegacyStorageLoadResult.success(Collections.<LegacyBeaconState>emptyList());
        }
        @Override public void store(Collection<LegacyBeaconState> beacons) {
            if (failWrites) throw new LegacyStorageException("synthetic failure");
        }
        @Override public void close() { }
        @Override public String getBackendName() { return "TEST"; }
    }
}
