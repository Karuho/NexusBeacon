package cl.dynasty.nexusbeacon.platform.legacy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.junit.jupiter.api.Test;

class LegacyPhase22ListenerContractTest {
    @Test void furnaceHandlersMatchModernCancellationContract() throws Exception {
        for (String method : new String[] { "onFurnaceBurn", "onFurnaceSmelt" }) {
            Method value = LegacyFurnaceBoostListener.class.getDeclaredMethods()[0];
            for (Method candidate : LegacyFurnaceBoostListener.class.getDeclaredMethods()) {
                if (candidate.getName().equals(method)) value = candidate;
            }
            EventHandler handler = value.getAnnotation(EventHandler.class);
            assertEquals(EventPriority.NORMAL, handler.priority());
            assertTrue(handler.ignoreCancelled());
        }
    }

    @Test void baseHandlerMatchesModernHighestObserveCancelledContract() throws Exception {
        Method method = LegacyBaseProtectionListener.class.getMethod("onBaseBlockBreak",
                org.bukkit.event.block.BlockBreakEvent.class);
        EventHandler handler = method.getAnnotation(EventHandler.class);
        assertEquals(EventPriority.HIGHEST, handler.priority());
        assertFalse(handler.ignoreCancelled());
    }
}
