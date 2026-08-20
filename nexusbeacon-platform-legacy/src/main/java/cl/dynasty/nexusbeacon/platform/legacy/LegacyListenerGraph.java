package cl.dynasty.nexusbeacon.platform.legacy;

import java.util.Arrays;

import org.bukkit.plugin.Plugin;

/** The safe registered subset is separate from state/effect-dependent deferred listeners. */
public final class LegacyListenerGraph {
    public static final int DEFERRED_PRODUCTION_LISTENERS = 3;

    private final LegacyPluginLifecycleListener lifecycleListener;
    private final LegacyVanillaBeaconListener vanillaBeaconListener;
    private final LegacyTransactionalBeaconListener transactionalBeaconListener;
    private final LegacyListenerRegistry registry;

    public LegacyListenerGraph(Plugin plugin, LegacyApplicationGraph application,
            boolean vanillaDisabled, String vanillaDisabledMessage,
            LegacyBeaconGameplaySettings gameplaySettings, LegacyBeaconItemFactory itemFactory,
            LegacyBeaconListenerMessages listenerMessages) {
        if (plugin == null) throw new NullPointerException("plugin");
        if (application == null) throw new NullPointerException("application");
        application.requireAvailable(LegacyApplicationCapability.IDENTITY);
        application.requireAvailable(LegacyApplicationCapability.MATERIAL);
        application.requireAvailable(LegacyApplicationCapability.MESSAGING);
        application.requireAvailable(LegacyApplicationCapability.CONFIGURATION);
        lifecycleListener = new LegacyPluginLifecycleListener(plugin);
        vanillaBeaconListener = new LegacyVanillaBeaconListener(application.getIdentities(),
                application.getMaterials(), application.getMessages(), vanillaDisabled, vanillaDisabledMessage);
        transactionalBeaconListener = new LegacyTransactionalBeaconListener(plugin, application,
                gameplaySettings, itemFactory, listenerMessages);
        registry = new LegacyListenerRegistry(plugin,
                Arrays.asList(lifecycleListener, vanillaBeaconListener, transactionalBeaconListener));
    }

    public boolean register() { return registry.register(); }
    public boolean unregister() { return registry.unregister(); }
    public boolean isRegistered() { return registry.isRegistered(); }
    public int getPortedListenerCount() { return registry.size(); }
    public int getDeferredListenerCount() { return DEFERRED_PRODUCTION_LISTENERS; }
    public boolean isEnableDispatchObserved() { return lifecycleListener.isEnableDispatchObserved(); }
    public LegacyVanillaBeaconListener getVanillaBeaconListener() { return vanillaBeaconListener; }
    public LegacyTransactionalBeaconListener getTransactionalBeaconListener() {
        return transactionalBeaconListener;
    }
}
