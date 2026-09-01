package cl.dynasty.nexusbeacon.gui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.junit.jupiter.api.Test;

import cl.dynasty.nexusbeacon.gui.framework.NexusGuiListener;
import cl.dynasty.nexusbeacon.listener.BeaconGuiListener;

class InventoryViewAbiCompatibilityTest {

    @Test
    void guiHandlersUseStableInventoryEventBoundaryWithoutInventoryViewInvocation() throws Exception {
        assertStableBoundary(NexusGuiListener.class);
        assertStableBoundary(BeaconGuiListener.class);
    }

    @Test
    void clickDragAndCreativeEventsReachTheExistingHandlerContracts() throws Exception {
        assertTrue(InventoryClickEvent.class.isAssignableFrom(InventoryCreativeEvent.class));
        assertTrue(NexusGuiListener.class.getMethod("onClick", InventoryClickEvent.class) != null);
        assertTrue(NexusGuiListener.class.getMethod("onDrag", InventoryDragEvent.class) != null);
        assertTrue(BeaconGuiListener.class.getMethod("onInventoryClick", InventoryClickEvent.class) != null);
        assertTrue(BeaconGuiListener.class.getMethod("onInventoryDrag", InventoryDragEvent.class) != null);
    }

    private void assertStableBoundary(Class<?> listenerClass) throws IOException {
        String resource = "/" + listenerClass.getName().replace('.', '/') + ".class";
        try (InputStream input = listenerClass.getResourceAsStream(resource)) {
            assertTrue(input != null, "missing listener bytecode: " + resource);
            String constantPool = new String(input.readAllBytes(), StandardCharsets.ISO_8859_1);
            assertTrue(constantPool.contains("getInventory"));
            assertFalse(constantPool.contains("org/bukkit/inventory/InventoryView"));
        }
    }
}
