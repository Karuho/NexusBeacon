package cl.dynasty.dynabeacon;

import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import cl.dynasty.dynabeacon.adapter.ModernAdapter;
import cl.dynasty.dynabeacon.adapter.ModernItemDataAdapter;
import cl.dynasty.dynabeacon.api.ItemDataAdapter;
import cl.dynasty.dynabeacon.api.VersionAdapter;
import cl.dynasty.dynabeacon.commands.DynaBeaconCommand;
import cl.dynasty.dynabeacon.config.ConfigManager;
import cl.dynasty.dynabeacon.effects.EffectRegistry;
import cl.dynasty.dynabeacon.effects.executor.EffectExecutorRegistry;
import cl.dynasty.dynabeacon.gui.BeaconGuiManager;
import cl.dynasty.dynabeacon.hooks.SpawnerMetaHook;
import cl.dynasty.dynabeacon.listener.BeaconGuiListener;
import cl.dynasty.dynabeacon.listener.BeaconListener;
import cl.dynasty.dynabeacon.listener.FurnaceBoostListener;
import cl.dynasty.dynabeacon.listener.VanillaBeaconListener;
import cl.dynasty.dynabeacon.manager.BeaconManager;
import cl.dynasty.dynabeacon.manager.BeaconPowerManager;
import cl.dynasty.dynabeacon.manager.CustomBeaconItemManager;
import cl.dynasty.dynabeacon.manager.CustomRecipeManager;
import cl.dynasty.dynabeacon.manager.PaymentManager;
import cl.dynasty.dynabeacon.manager.PlayerSettingsManager;
import cl.dynasty.dynabeacon.storage.StorageManager;
import cl.dynasty.dynabeacon.task.BeaconParticleTask;
import cl.dynasty.dynabeacon.task.BeaconTickTask;
import net.milkbowl.vault.economy.Economy;

public final class DynaBeaconPlugin extends JavaPlugin {

    private static DynaBeaconPlugin instance;

    private VersionAdapter versionAdapter;
    private ConfigManager configManager;
    private StorageManager storageManager;
    private BeaconManager beaconManager;
    private PlayerSettingsManager playerSettingsManager;
    private EffectRegistry effectRegistry;
    private BeaconGuiManager beaconGuiManager;
    private CustomBeaconItemManager customBeaconItemManager;
    private BeaconPowerManager beaconPowerManager;
    private PaymentManager paymentManager;
    private ItemDataAdapter itemDataAdapter;
    private Economy economy;
    private CustomRecipeManager customRecipeManager;
    private EffectExecutorRegistry effectExecutorRegistry;
    private SpawnerMetaHook spawnerMetaHook;

    public static DynaBeaconPlugin getInstance() {
        return instance;
    }

    public VersionAdapter getVersionAdapter() {
        return versionAdapter;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public StorageManager getStorageManager() {
        return storageManager;
    }

    public BeaconManager getBeaconManager() {
        return beaconManager;
    }

    public PlayerSettingsManager getPlayerSettingsManager() {
        return playerSettingsManager;
    }

    public EffectRegistry getEffectRegistry() {
        return effectRegistry;
    }

    public BeaconGuiManager getBeaconGuiManager() {
        return beaconGuiManager;
    }

    public CustomBeaconItemManager getCustomBeaconItemManager() {
        return customBeaconItemManager;
    }

    public BeaconPowerManager getBeaconPowerManager() {
        return beaconPowerManager;
    }

    public PaymentManager getPaymentManager() {
        return paymentManager;
    }

    public ItemDataAdapter getItemDataAdapter() {
        return itemDataAdapter;
    }

    public Economy getEconomy() {
        return economy;
    }

    public CustomRecipeManager getCustomRecipeManager() {
        return customRecipeManager;
    }

    public EffectExecutorRegistry getEffectExecutorRegistry() {
        return effectExecutorRegistry;
    }

    public SpawnerMetaHook getSpawnerMetaHook() {
        return spawnerMetaHook;
    }

    @Override
    public void onEnable() {
        instance = this;

        versionAdapter = new ModernAdapter();
        configManager = new ConfigManager(this);
        storageManager = new StorageManager(this);
        beaconManager = new BeaconManager(this);
        playerSettingsManager = new PlayerSettingsManager(this);
        effectRegistry = new EffectRegistry(this);
        effectExecutorRegistry = new EffectExecutorRegistry(this);
        beaconGuiManager = new BeaconGuiManager(this);
        if (ModernItemDataAdapter.isSupported()) {
            itemDataAdapter = new ModernItemDataAdapter(this);
        } else {
            getLogger().warning("PersistentDataContainer no disponible. DynaBeacon necesita adapter legacy.");
        }
        customBeaconItemManager = new CustomBeaconItemManager(this);
        customRecipeManager = new CustomRecipeManager(this);
        beaconPowerManager = new BeaconPowerManager(this);
        setupEconomy();
        paymentManager = new PaymentManager(this);
        spawnerMetaHook = new SpawnerMetaHook();
        spawnerMetaHook.load();

        if (spawnerMetaHook.isEnabled()) {
            getLogger().info("SpawnerMeta detectado. Hook de spawners activado.");
        }
        reloadAll();
        customRecipeManager.load();

        DynaBeaconCommand command = new DynaBeaconCommand(this);
        getCommand("dynabeacon").setExecutor(command);
        getCommand("dynabeacon").setTabCompleter(command);
        getServer().getPluginManager().registerEvents(new FurnaceBoostListener(this), this);
        Bukkit.getPluginManager().registerEvents(new BeaconListener(this), this);
        Bukkit.getPluginManager().registerEvents(new BeaconGuiListener(this), this);
        Bukkit.getPluginManager().registerEvents(new VanillaBeaconListener(this), this);

        int interval = configManager.getBeaconConfig().getInt("beacon.tick-interval", 40);
        new BeaconTickTask(this).runTaskTimer(this, interval, interval);

        int particleInterval = configManager.getBeaconConfig().getInt("beacon.particles.interval-ticks", 20);
        new BeaconParticleTask(this).runTaskTimer(this, particleInterval, particleInterval);

        getLogger().info("DynaBeacon activado correctamente.");
        getLogger().info("Versión Bukkit detectada: " + Bukkit.getBukkitVersion());
        getLogger().info("Adapter activo: ModernAdapter");
    }

    @Override
    public void onDisable() {
        getLogger().info("DynaBeacon desactivado correctamente.");
    }

    public void reloadAll() {
        configManager.load();
        effectRegistry.load();
        effectExecutorRegistry.load();
        beaconManager.load();
    }


    private void setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            getLogger().warning("Vault no encontrado. Pagos por dinero desactivados.");
            return;
        }

        RegisteredServiceProvider<Economy> provider = getServer().getServicesManager().getRegistration(Economy.class);

        if (provider == null) {
            getLogger().warning("No se encontró proveedor de economía para Vault.");
            return;
        }

        economy = provider.getProvider();
        getLogger().info("Economía Vault conectada correctamente.");
    }
}