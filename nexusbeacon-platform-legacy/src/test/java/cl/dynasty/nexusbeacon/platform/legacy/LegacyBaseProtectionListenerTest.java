package cl.dynasty.nexusbeacon.platform.legacy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.junit.jupiter.api.Test;

class LegacyBaseProtectionListenerTest {
    @Test void untrustedBreakIsCancelledButOwnerTrustedAndAdminAreAllowed() {
        Fixture fixture = new Fixture(true, true);
        Block base = block("world", 1, 63, 1, Material.DIAMOND_BLOCK);

        BlockBreakEvent untrusted = new BlockBreakEvent(base, player(UUID.randomUUID(), false));
        fixture.listener.onBaseBlockBreak(untrusted);
        assertTrue(untrusted.isCancelled());

        BlockBreakEvent owner = new BlockBreakEvent(base, player(fixture.owner, false));
        fixture.listener.onBaseBlockBreak(owner);
        assertFalse(owner.isCancelled());

        BlockBreakEvent trusted = new BlockBreakEvent(base, player(fixture.trusted, false));
        fixture.listener.onBaseBlockBreak(trusted);
        assertFalse(trusted.isCancelled());

        BlockBreakEvent admin = new BlockBreakEvent(base, player(UUID.randomUUID(), true));
        fixture.listener.onBaseBlockBreak(admin);
        assertFalse(admin.isCancelled());
    }

    @Test void globalOrBeaconProtectionDisabledLeavesBaseUnprotected() {
        Block base = block("world", 0, 63, 0, Material.IRON_BLOCK);
        Fixture globalOff = new Fixture(false, true);
        assertNull(globalOff.listener.findProtectingBeacon(base));
        Fixture beaconOff = new Fixture(true, false);
        assertNull(beaconOff.listener.findProtectingBeacon(base));
    }

    @Test void geometryWorldVanillaAndRemovalControlsUseOnlyAuthoritativeState() {
        Fixture fixture = new Fixture(true, true);
        assertSame(fixture.beacon, fixture.listener.findProtectingBeacon(
                block("world", -4, 60, 4, Material.EMERALD_BLOCK)));
        assertNull(fixture.listener.findProtectingBeacon(block("world", 5, 60, 0, Material.EMERALD_BLOCK)));
        assertNull(fixture.listener.findProtectingBeacon(block("other", 0, 63, 0, Material.EMERALD_BLOCK)));
        assertNull(fixture.listener.findProtectingBeacon(block("world", 100, 63, 100, Material.EMERALD_BLOCK)));
        fixture.state.delete(fixture.beacon.getLocation());
        assertNull(fixture.listener.findProtectingBeacon(block("world", 0, 63, 0, Material.EMERALD_BLOCK)));
    }

    @Test void priorCancellationIsNeverUncancelled() {
        Fixture fixture = new Fixture(true, true);
        BlockBreakEvent event = new BlockBreakEvent(block("world", 0, 63, 0, Material.IRON_BLOCK),
                player(fixture.owner, false));
        event.setCancelled(true);
        fixture.listener.onBaseBlockBreak(event);
        assertTrue(event.isCancelled());
    }

    private static final class Fixture {
        private final UUID owner = UUID.randomUUID();
        private final UUID trusted = UUID.randomUUID();
        private final Storage storage = new Storage();
        private final LegacyApplicationState state = new LegacyApplicationState(storage);
        private final LegacyBeaconState beacon;
        private final LegacyBaseProtectionListener listener;
        private Fixture(boolean global, boolean beaconProtection) {
            Set<UUID> trustedPlayers = new LinkedHashSet<UUID>();
            trustedPlayers.add(trusted);
            beacon = new LegacyBeaconState(new LegacyBeaconLocation("world", 0, 64, 0), UUID.randomUUID(), owner,
                    48, 1, Collections.<String, Integer>emptyMap(), Collections.<String>emptySet(), trustedPlayers,
                    beaconProtection, "aqua", true, "VILLAGER_HAPPY");
            storage.values.add(beacon);
            state.initialize();
            listener = new LegacyBaseProtectionListener(state,
                    new LegacyMessageService(new LegacyTextFormatter()), global, 4, "protected");
        }
    }

    private static Player player(final UUID uuid, final boolean admin) {
        return (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[] { Player.class },
                (proxy, method, args) -> "getUniqueId".equals(method.getName()) ? uuid
                        : "hasPermission".equals(method.getName()) ? Boolean.valueOf(admin)
                        : primitive(method.getReturnType()));
    }

    private static Block block(final String worldName, final int x, final int y, final int z, final Material type) {
        final World world = (World) Proxy.newProxyInstance(World.class.getClassLoader(), new Class<?>[] { World.class },
                (proxy, method, args) -> "getName".equals(method.getName()) ? worldName : primitive(method.getReturnType()));
        return (Block) Proxy.newProxyInstance(Block.class.getClassLoader(), new Class<?>[] { Block.class },
                (proxy, method, args) -> "getWorld".equals(method.getName()) ? world
                        : "getX".equals(method.getName()) ? Integer.valueOf(x)
                        : "getY".equals(method.getName()) ? Integer.valueOf(y)
                        : "getZ".equals(method.getName()) ? Integer.valueOf(z)
                        : "getType".equals(method.getName()) ? type : primitive(method.getReturnType()));
    }

    private static Object primitive(Class<?> type) {
        if (type == Boolean.TYPE) return Boolean.FALSE;
        if (type == Integer.TYPE) return Integer.valueOf(0);
        if (type == Short.TYPE) return Short.valueOf((short) 0);
        if (type == Byte.TYPE) return Byte.valueOf((byte) 0);
        if (type == Long.TYPE) return Long.valueOf(0L);
        if (type == Float.TYPE) return Float.valueOf(0F);
        if (type == Double.TYPE) return Double.valueOf(0D);
        return null;
    }

    private static final class Storage implements LegacyBeaconStorage {
        private final List<LegacyBeaconState> values = new ArrayList<LegacyBeaconState>();
        @Override public LegacyStorageLoadResult load() { return LegacyStorageLoadResult.success(values); }
        @Override public void store(Collection<LegacyBeaconState> beacons) { values.clear(); values.addAll(beacons); }
        @Override public void close() { }
        @Override public String getBackendName() { return "TEST"; }
    }
}
