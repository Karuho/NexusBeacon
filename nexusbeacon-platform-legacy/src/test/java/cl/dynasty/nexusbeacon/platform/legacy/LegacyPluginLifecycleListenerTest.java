package cl.dynasty.nexusbeacon.platform.legacy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.logging.Logger;

import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;

class LegacyPluginLifecycleListenerTest {
    @Test
    void observesOnlyItsOwnersRealBukkitEventType() {
        Plugin owner = plugin("owner");
        LegacyPluginLifecycleListener listener = new LegacyPluginLifecycleListener(owner);

        listener.onPluginEnable(new PluginEnableEvent(plugin("other")));
        assertFalse(listener.isEnableDispatchObserved());

        listener.onPluginEnable(new PluginEnableEvent(owner));
        assertTrue(listener.isEnableDispatchObserved());
    }

    private Plugin plugin(String name) {
        Logger logger = Logger.getLogger("LegacyPluginLifecycleListenerTest." + name);
        return (Plugin) Proxy.newProxyInstance(Plugin.class.getClassLoader(), new Class<?>[] { Plugin.class },
                (instance, method, args) -> {
                    if (method.getName().equals("getLogger")) return logger;
                    if (method.getName().equals("getName")) return name;
                    Class<?> returnType = method.getReturnType();
                    if (returnType == boolean.class) return false;
                    if (returnType == int.class) return 0;
                    if (returnType == long.class) return 0L;
                    return null;
                });
    }
}
