package cl.dynasty.nexusbeacon.platform.legacy;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;

/** Tracks Bukkit acknowledgement of this plugin's registered application listener graph. */
public final class LegacyPluginLifecycleListener implements Listener {
    private final Plugin owner;
    private boolean enableDispatchObserved;

    public LegacyPluginLifecycleListener(Plugin owner) {
        if (owner == null) throw new NullPointerException("owner");
        this.owner = owner;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPluginEnable(PluginEnableEvent event) {
        if (event == null || event.getPlugin() != owner) return;
        enableDispatchObserved = true;
        owner.getLogger().info("Legacy listener dispatch confirmed: PluginEnableEvent.");
    }

    public boolean isEnableDispatchObserved() {
        return enableDispatchObserved;
    }
}
