package cl.dynasty.nexusbeacon;

import org.bukkit.plugin.java.JavaPlugin;

import cl.dynasty.nexusbeacon.api.ItemDataAdapter;
import cl.dynasty.nexusbeacon.api.VersionAdapter;
import cl.dynasty.nexusbeacon.beam.BeamStyleManager;
import cl.dynasty.nexusbeacon.bootstrap.ModernPluginBootstrap;
import cl.dynasty.nexusbeacon.config.ConfigManager;
import cl.dynasty.nexusbeacon.effects.EffectRegistry;
import cl.dynasty.nexusbeacon.effects.executor.EffectExecutorRegistry;
import cl.dynasty.nexusbeacon.gui.BeaconGuiManager;
import cl.dynasty.nexusbeacon.hooks.SpawnerMetaHook;
import cl.dynasty.nexusbeacon.manager.BeaconManager;
import cl.dynasty.nexusbeacon.manager.BeaconPowerManager;
import cl.dynasty.nexusbeacon.manager.CustomBeaconItemManager;
import cl.dynasty.nexusbeacon.manager.CustomRecipeManager;
import cl.dynasty.nexusbeacon.manager.LanguageManager;
import cl.dynasty.nexusbeacon.manager.PaymentManager;
import cl.dynasty.nexusbeacon.manager.PlayerSettingsManager;
import cl.dynasty.nexusbeacon.platform.PlatformDescriptor;
import cl.dynasty.nexusbeacon.platform.modern.ModernVersionSelection;
import cl.dynasty.nexusbeacon.service.ParticleService;
import cl.dynasty.nexusbeacon.platform.api.SchedulerService;
import cl.dynasty.nexusbeacon.platform.api.TeleporterService;
import cl.dynasty.nexusbeacon.storage.StorageManager;
import net.milkbowl.vault.economy.Economy;

public final class NexusBeaconPlugin extends JavaPlugin {

    private static NexusBeaconPlugin instance;
    private ModernPluginBootstrap bootstrap;

    public static NexusBeaconPlugin getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        bootstrap = new ModernPluginBootstrap(this);
        bootstrap.enable();
    }

    @Override
    public void onDisable() {
        if (bootstrap != null) {
            bootstrap.disable();
        } else {
            getLogger().info("NexusBeacon successfully disabled.");
        }
    }

    public void reloadAll() {
        requireBootstrap().reloadAll();
    }

    public void restartRuntimeTasks() {
        requireBootstrap().restartRuntimeTasks();
    }

    public void restartVisualBeamTask() {
        requireBootstrap().restartVisualBeamTask();
    }

    public VersionAdapter getVersionAdapter() { return requireBootstrap().getVersionAdapter(); }
    public ConfigManager getConfigManager() { return requireBootstrap().getConfigManager(); }
    public LanguageManager getLanguageManager() { return requireBootstrap().getLanguageManager(); }
    public StorageManager getStorageManager() { return requireBootstrap().getStorageManager(); }
    public BeaconManager getBeaconManager() { return requireBootstrap().getBeaconManager(); }
    public PlayerSettingsManager getPlayerSettingsManager() { return requireBootstrap().getPlayerSettingsManager(); }
    public EffectRegistry getEffectRegistry() { return requireBootstrap().getEffectRegistry(); }
    public BeaconGuiManager getBeaconGuiManager() { return requireBootstrap().getBeaconGuiManager(); }
    public CustomBeaconItemManager getCustomBeaconItemManager() { return requireBootstrap().getCustomBeaconItemManager(); }
    public BeaconPowerManager getBeaconPowerManager() { return requireBootstrap().getBeaconPowerManager(); }
    public PaymentManager getPaymentManager() { return requireBootstrap().getPaymentManager(); }
    public ItemDataAdapter getItemDataAdapter() { return requireBootstrap().getItemDataAdapter(); }
    public Economy getEconomy() { return requireBootstrap().getEconomy(); }
    public CustomRecipeManager getCustomRecipeManager() { return requireBootstrap().getCustomRecipeManager(); }
    public EffectExecutorRegistry getEffectExecutorRegistry() { return requireBootstrap().getEffectExecutorRegistry(); }
    public SpawnerMetaHook getSpawnerMetaHook() { return requireBootstrap().getSpawnerMetaHook(); }
    public SchedulerService getSchedulerService() { return requireBootstrap().getSchedulerService(); }
    public TeleporterService getTeleporterService() { return requireBootstrap().getTeleporterService(); }
    public ParticleService getParticleService() { return requireBootstrap().getParticleService(); }
    public BeamStyleManager getBeamStyleManager() { return requireBootstrap().getBeamStyleManager(); }
    public PlatformDescriptor getPlatformDescriptor() { return requireBootstrap().getPlatformDescriptor(); }
    public ModernVersionSelection getModernVersionSelection() { return requireBootstrap().getModernVersionSelection(); }

    public boolean isDebugEnabled() {
        ConfigManager manager = bootstrap == null ? null : bootstrap.getConfigManager();
        return manager != null && manager.getConfig().getBoolean("debug", false);
    }

    private ModernPluginBootstrap requireBootstrap() {
        if (bootstrap == null) {
            throw new IllegalStateException("NexusBeacon bootstrap is not initialized");
        }
        return bootstrap;
    }
}
