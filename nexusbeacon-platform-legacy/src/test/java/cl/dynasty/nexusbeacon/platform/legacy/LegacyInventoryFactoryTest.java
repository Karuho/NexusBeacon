package cl.dynasty.nexusbeacon.platform.legacy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.junit.jupiter.api.Test;

class LegacyInventoryFactoryTest {
    @Test
    void createsValidInventoryWithExactHolderSizeAndTitle() {
        final Inventory expected = proxy(Inventory.class);
        final InventoryHolder holder = proxy(InventoryHolder.class);
        final AtomicReference<InventoryHolder> actualHolder = new AtomicReference<InventoryHolder>();
        final AtomicReference<String> actualTitle = new AtomicReference<String>();
        final AtomicInteger actualSize = new AtomicInteger();
        LegacyInventoryFactory factory = new LegacyInventoryFactory(new LegacyInventoryFactory.InventoryCreator() {
            @Override public Inventory create(InventoryHolder suppliedHolder, int size, String title) {
                actualHolder.set(suppliedHolder);
                actualSize.set(size);
                actualTitle.set(title);
                return expected;
            }
        });

        assertSame(expected, factory.create(holder, 54, "\u00a78NexusBeacon - Effects"));
        assertSame(holder, actualHolder.get());
        assertEquals(54, actualSize.get());
        assertEquals("\u00a78NexusBeacon - Effects", actualTitle.get());
    }

    @Test
    void rejectsSizesOutsideLegacyChestRange() {
        assertThrows(IllegalArgumentException.class, () -> LegacyInventoryFactory.validate(0, "x"));
        assertThrows(IllegalArgumentException.class, () -> LegacyInventoryFactory.validate(63, "x"));
    }

    @Test
    void rejectsNonRowAlignedSize() {
        assertThrows(IllegalArgumentException.class, () -> LegacyInventoryFactory.validate(10, "x"));
    }

    @Test
    void rejectsNullAndOverlongTitlesWithoutTruncation() {
        assertThrows(IllegalArgumentException.class, () -> LegacyInventoryFactory.validate(9, null));
        assertThrows(IllegalArgumentException.class, () -> LegacyInventoryFactory.validate(9,
                "123456789012345678901234567890123"));
    }

    @Test
    void acceptsMaximumTitleLengthExactly() {
        LegacyInventoryFactory.validate(9, "12345678901234567890123456789012");
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type },
                (instance, method, args) -> {
                    Class<?> returnType = method.getReturnType();
                    if (returnType == boolean.class) return false;
                    if (returnType == int.class) return 0;
                    if (returnType == long.class) return 0L;
                    return null;
                });
    }
}
