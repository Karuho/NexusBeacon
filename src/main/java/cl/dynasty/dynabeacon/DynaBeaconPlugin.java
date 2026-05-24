package cl.dynasty.dynabeacon;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.player.PlayerTeleportEvent;
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
import cl.dynasty.dynabeacon.service.FoliaSchedulerService;
import cl.dynasty.dynabeacon.service.PaperTeleporterService;
import cl.dynasty.dynabeacon.service.ParticleService;
import cl.dynasty.dynabeacon.service.SchedulerService;
import cl.dynasty.dynabeacon.service.SpigotSchedulerService;
import cl.dynasty.dynabeacon.service.SpigotTeleporterService;
import cl.dynasty.dynabeacon.service.TeleporterService;
import cl.dynasty.dynabeacon.storage.StorageManager;
import cl.dynasty.dynabeacon.task.BeaconParticleTask;
import cl.dynasty.dynabeacon.task.BeaconTickTask;
import net.milkbowl.vault.economy.Economy;

public final class DynaBeaconPlugin extends JavaPlugin {

    private static DynaBeaconPlugin instance;

    private void printStartupBanner() {
        getLogger().info("_______________________________________________________");
        getLogger().info("");
        getLogger().info(" ███╗   ██╗███████╗██╗  ██╗██╗   ██╗███████╗");
        getLogger().info(" ████╗  ██║██╔════╝╚██╗██╔╝██║   ██║██╔════╝");
        getLogger().info(" ██╔██╗ ██║█████╗   ╚███╔╝ ██║   ██║███████╗");
        getLogger().info(" ██║╚██╗██║██╔══╝   ██╔██╗ ██║   ██║╚════██║");
        getLogger().info(" ██║ ╚████║███████╗██╔╝ ██╗╚██████╔╝███████║");
        getLogger().info(" ╚═╝  ╚═══╝╚══════╝╚═╝  ╚═╝ ╚═════╝ ╚══════╝");
        getLogger().info("");
        getLogger().info(" NexusBeacon v" + getDescription().getVersion());
        getLogger().info(" Running on " + getServer().getName() + " - " + getServer().getVersion());
        getLogger().info("_______________________________________________________");
    }

    private VersionAdapter versionAdapter;
    private ConfigManager configManager;
    private cl.dynasty.dynabeacon.storage.StorageManager storageManager;
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
    private SchedulerService schedulerService;
    private cl.dynasty.dynabeacon.service.ScheduledTaskHandle beaconTickHandle;
    private cl.dynasty.dynabeacon.service.ScheduledTaskHandle beaconParticleHandle;
    private TeleporterService teleporterService;
    private ParticleService particleService;

    public static DynaBeaconPlugin getInstance() {
        return instance;
    }

    public VersionAdapter getVersionAdapter() {
        return versionAdapter;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public cl.dynasty.dynabeacon.storage.StorageManager getStorageManager() {
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

    public SchedulerService getSchedulerService() {
        return schedulerService;
    }

    public TeleporterService getTeleporterService() {
        return teleporterService;
    }

    public ParticleService getParticleService() {
        return particleService;
    }

    @Override
    public void onEnable() {
        instance = this;

        printStartupBanner();
        schedulerService = createSchedulerService();
        teleporterService = createTeleporterService();
        particleService = new ParticleService();
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

        int particleInterval = configManager.getBeaconConfig().getInt("beacon.particles.interval-ticks", 20);

        if (beaconTickHandle != null) {
            beaconTickHandle.cancel();
        }

        if (beaconParticleHandle != null) {
            beaconParticleHandle.cancel();
        }

        beaconTickHandle = schedulerService.runSyncTimer(
                new BeaconTickTask(this),
                interval,
                interval);

        beaconParticleHandle = schedulerService.runSyncTimer(
                new BeaconParticleTask(this),
                particleInterval,
                particleInterval);

        getLogger().info("DynaBeacon activado correctamente.");
        getLogger().info("Versión Bukkit detectada: " + Bukkit.getBukkitVersion());
        getLogger().info("Adapter activo: ModernAdapter");
    }

    @Override
    public void onDisable() {
        if (beaconTickHandle != null) {
            beaconTickHandle.cancel();
        }

        if (beaconParticleHandle != null) {
            beaconParticleHandle.cancel();
        }

        if (storageManager != null) {
            storageManager.close();
        }

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

    private SchedulerService createSchedulerService() {
        try {
            Class.forName("io.papermc.paper.threadedregions.scheduler.ScheduledTask");
            getLogger().info("Scheduler activo: Folia");
            return new FoliaSchedulerService(this);
        } catch (ClassNotFoundException exception) {
            getLogger().info("Scheduler activo: Bukkit/Paper clásico");
            return new SpigotSchedulerService(this);
        }
    }

    private TeleporterService createTeleporterService() {
        try {
            Entity.class.getMethod("teleportAsync", Location.class, PlayerTeleportEvent.TeleportCause.class);
            getLogger().info("Teleporter activo: Paper async");
            return new PaperTeleporterService(schedulerService);
        } catch (NoSuchMethodException exception) {
            getLogger().info("Teleporter activo: Bukkit sync");
            return new SpigotTeleporterService(schedulerService);
        }
    }
}