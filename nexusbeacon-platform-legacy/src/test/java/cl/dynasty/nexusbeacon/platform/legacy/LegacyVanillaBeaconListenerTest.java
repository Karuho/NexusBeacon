package cl.dynasty.nexusbeacon.platform.legacy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

class LegacyVanillaBeaconListenerTest {
    @Test
    void cancelsVanillaBeaconWhenPolicyDisablesItAndSendsLocalizedMessage() {
        AtomicReference<String> sent = new AtomicReference<String>();
        BlockPlaceEvent event = event(Material.BEACON, new ItemStack(Material.BEACON), player(sent));

        listener(LegacyIdentityStatus.NOT_RECOGNIZED, true).onVanillaBeaconPlace(event);

        assertTrue(event.isCancelled());
        assertEquals("\u00a7cVanilla disabled", sent.get());
    }

    @Test
    void permitsRecognizedCustomBeaconUnderVanillaDisabledPolicy() {
        AtomicReference<String> sent = new AtomicReference<String>();
        BlockPlaceEvent event = event(Material.BEACON, new ItemStack(Material.BEACON), player(sent));

        listener(LegacyIdentityStatus.RECOGNIZED, true).onVanillaBeaconPlace(event);

        assertFalse(event.isCancelled());
        assertEquals(null, sent.get());
    }

    @Test
    void treatsMalformedIdentityAsVanillaAndFailsClosed() {
        BlockPlaceEvent event = event(Material.BEACON, new ItemStack(Material.BEACON), player(new AtomicReference<String>()));
        listener(LegacyIdentityStatus.MALFORMED, true).onVanillaBeaconPlace(event);
        assertTrue(event.isCancelled());
    }

    @Test
    void ignoresNonBeaconPlacement() {
        AtomicReference<String> sent = new AtomicReference<String>();
        BlockPlaceEvent event = event(Material.STONE, new ItemStack(Material.STONE), player(sent));
        listener(LegacyIdentityStatus.NOT_RECOGNIZED, true).onVanillaBeaconPlace(event);
        assertFalse(event.isCancelled());
        assertEquals(null, sent.get());
    }

    @Test
    void neverUncancelsAnEventCancelledByAnotherPlugin() {
        BlockPlaceEvent event = event(Material.BEACON, new ItemStack(Material.BEACON), player(new AtomicReference<String>()));
        event.setCancelled(true);
        listener(LegacyIdentityStatus.NOT_RECOGNIZED, false).onVanillaBeaconPlace(event);
        assertTrue(event.isCancelled());
    }

    private LegacyVanillaBeaconListener listener(LegacyIdentityStatus status, boolean disabled) {
        LegacyNbtBridge bridge = new LegacyNbtBridge() {
            @Override public ItemStack mark(ItemStack item) { return item; }
            @Override public LegacyIdentityStatus identify(ItemStack item) { return status; }
            @Override public String getRevision() { return "test"; }
        };
        return new LegacyVanillaBeaconListener(new LegacyItemIdentityService(bridge),
                new LegacyMaterialResolver(), new LegacyMessageService(new LegacyTextFormatter()),
                disabled, "&cVanilla disabled");
    }

    private BlockPlaceEvent event(Material type, ItemStack item, Player player) {
        Block block = proxy(Block.class, type);
        BlockState state = proxy(BlockState.class, type);
        return new BlockPlaceEvent(block, state, block, item, player, true);
    }

    private Player player(AtomicReference<String> sent) {
        return (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[] { Player.class },
                (instance, method, args) -> {
                    if (method.getName().equals("sendMessage") && args != null && args.length == 1
                            && args[0] instanceof String) sent.set((String) args[0]);
                    return defaultValue(method.getReturnType());
                });
    }

    private <T> T proxy(Class<T> type, Material material) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type },
                (instance, method, args) -> method.getName().equals("getType")
                        ? material : defaultValue(method.getReturnType())));
    }

    private Object defaultValue(Class<?> type) {
        if (type == boolean.class) return false;
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0F;
        if (type == double.class) return 0D;
        if (type == char.class) return '\0';
        return null;
    }
}
