package cl.dynasty.nexusbeacon;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import cl.dynasty.nexusbeacon.adapter.ModernAdapter;
import cl.dynasty.nexusbeacon.adapter.ModernItemDataAdapter;
import cl.dynasty.nexusbeacon.api.ItemDataAdapter;
import cl.dynasty.nexusbeacon.api.VersionAdapter;
import cl.dynasty.nexusbeacon.beam.BeamStyleManager;
import cl.dynasty.nexusbeacon.commands.NexusBeaconCommand;
import cl.dynasty.nexusbeacon.config.ConfigManager;
import cl.dynasty.nexusbeacon.effects.EffectRegistry;
import cl.dynasty.nexusbeacon.effects.executor.EffectExecutorRegistry;
import cl.dynasty.nexusbeacon.gui.BeaconGuiManager;
import cl.dynasty.nexusbeacon.gui.framework.NexusGuiListener;
import cl.dynasty.nexusbeacon.hooks.SpawnerMetaHook;
import cl.dynasty.nexusbeacon.hooks.placeholderapi.NexusBeaconPlaceholderExpansion;
import cl.dynasty.nexusbeacon.listener.BeaconGuiListener;
import cl.dynasty.nexusbeacon.listener.BeaconListener;
import cl.dynasty.nexusbeacon.listener.FurnaceBoostListener;
import cl.dynasty.nexusbeacon.listener.VanillaBeaconListener;
import cl.dynasty.nexusbeacon.manager.BeaconManager;
import cl.dynasty.nexusbeacon.manager.BeaconPowerManager;
import cl.dynasty.nexusbeacon.manager.CustomBeaconItemManager;
import cl.dynasty.nexusbeacon.manager.CustomRecipeManager;
import cl.dynasty.nexusbeacon.manager.LanguageManager;
import cl.dynasty.nexusbeacon.manager.PaymentManager;
import cl.dynasty.nexusbeacon.manager.PlayerSettingsManager;
import cl.dynasty.nexusbeacon.service.FoliaSchedulerService;
import cl.dynasty.nexusbeacon.service.PaperTeleporterService;
import cl.dynasty.nexusbeacon.service.ParticleService;
import cl.dynasty.nexusbeacon.service.SchedulerService;
import cl.dynasty.nexusbeacon.service.SpigotSchedulerService;
import cl.dynasty.nexusbeacon.service.SpigotTeleporterService;
import cl.dynasty.nexusbeacon.service.TeleporterService;
import cl.dynasty.nexusbeacon.storage.StorageManager;
import cl.dynasty.nexusbeacon.task.BeaconParticleTask;
import cl.dynasty.nexusbeacon.task.BeaconTickTask;
import cl.dynasty.nexusbeacon.task.BeaconVisualBeamTask;
import net.milkbowl.vault.economy.Economy;

public final class NexusBeaconPlugin extends JavaPlugin {

    private static NexusBeaconPlugin instance;

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
    private LanguageManager languageManager;
    private cl.dynasty.nexusbeacon.storage.StorageManager storageManager;
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
    private cl.dynasty.nexusbeacon.service.ScheduledTaskHandle beaconTickHandle;
    private cl.dynasty.nexusbeacon.service.ScheduledTaskHandle beaconParticleHandle;
    private TeleporterService teleporterService;
    private ParticleService particleService;
    private NexusBeaconPlaceholderExpansion placeholderExpansion;
    private BeamStyleManager beamStyleManager;
    private cl.dynasty.nexusbeacon.service.ScheduledTaskHandle visualBeamHandle;

    public static NexusBeaconPlugin getInstance() {
        return instance;
    }

    public VersionAdapter getVersionAdapter() {
        return versionAdapter;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    public cl.dynasty.nexusbeacon.storage.StorageManager getStorageManager() {
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

    public BeamStyleManager getBeamStyleManager() {
        return beamStyleManager;
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
        configManager.load();

        languageManager = new LanguageManager(this);
        languageManager.load();

        beamStyleManager = new BeamStyleManager(this);
        beamStyleManager.load();

        storageManager = new StorageManager(this);

        effectRegistry = new EffectRegistry(this);
        effectExecutorRegistry = new EffectExecutorRegistry(this);

        beaconManager = new BeaconManager(this);
        playerSettingsManager = new PlayerSettingsManager(this);
        beaconGuiManager = new BeaconGuiManager(this);
        customBeaconItemManager = new CustomBeaconItemManager(this);
        customRecipeManager = new CustomRecipeManager(this);
        beaconPowerManager = new BeaconPowerManager(this);

        if (ModernItemDataAdapter.isSupported()) {
            itemDataAdapter = new ModernItemDataAdapter(this);
        } else {
            getLogger().severe(languageManager.get("console.adapter.persistent-data-unavailable"));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        setupEconomy();
        paymentManager = new PaymentManager(this);

        spawnerMetaHook = new SpawnerMetaHook();
        spawnerMetaHook.load();

        if (spawnerMetaHook.isEnabled()) {
            getLogger().info("SpawnerMeta detected. Spawners hook activated.");
        }

        reloadAll();
        restartVisualBeamTask();
        registerPlaceholderApi();
        customRecipeManager.load();

        NexusBeaconCommand command = new NexusBeaconCommand(this);
        getCommand("NexusBeacon").setExecutor(command);
        getCommand("NexusBeacon").setTabCompleter(command);

        getServer().getPluginManager().registerEvents(new FurnaceBoostListener(this), this);
        Bukkit.getPluginManager().registerEvents(new BeaconListener(this), this);
        Bukkit.getPluginManager().registerEvents(new BeaconGuiListener(this), this);
        Bukkit.getPluginManager().registerEvents(new VanillaBeaconListener(this), this);
        Bukkit.getPluginManager().registerEvents(new NexusGuiListener(), this);

        int interval = configManager.getBeaconConfig().getInt("beacon.tick-interval", 40);
        int particleInterval = configManager.getBeaconConfig().getInt("beacon.particles.interval-ticks", 20);

        restartVisualBeamTask();

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

        getLogger().info("NexusBeacon successfully activated.");
        getLogger().info("Bukkit version detected: " + Bukkit.getBukkitVersion());
        getLogger().info("Active adapter: ModernAdapter");
    }

    @Override
    public void onDisable() {
        if (beaconTickHandle != null) {
            beaconTickHandle.cancel();
        }

        if (beaconParticleHandle != null) {
            beaconParticleHandle.cancel();
        }

        if (visualBeamHandle != null) {
            visualBeamHandle.cancel();
        }

        if (placeholderExpansion != null) {
            placeholderExpansion.unregister();
        }

        if (storageManager != null) {
            storageManager.close();
        }

        getLogger().info("NexusBeacon successfully disabled.");
    }

    public void reloadAll() {
        languageManager.load();
        beamStyleManager.load();
        effectRegistry.load();
        effectExecutorRegistry.load();
        beaconManager.load();
    }

    public void restartVisualBeamTask() {
        if (visualBeamHandle != null) {
            visualBeamHandle.cancel();
        }

        int visualBeamInterval = configManager.getBeaconConfig().getInt("visual-beam.interval-ticks", 4);

        visualBeamHandle = schedulerService.runSyncTimer(
                new BeaconVisualBeamTask(this),
                visualBeamInterval,
                visualBeamInterval);
    }

    private void setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            getLogger().warning("Vault is not installed/loaded. Money payments disabled.");
            return;
        }

        if (!Bukkit.getPluginManager().isPluginEnabled("Vault")) {
            getLogger().warning("Vault is installed, but not yet enabled.");
            return;
        }

        RegisteredServiceProvider<Economy> provider = getServer().getServicesManager().getRegistration(Economy.class);

        if (provider == null) {
            getLogger().warning("Vault is active, but there is no economy plugin registered with Vault.");
            return;
        }

        economy = provider.getProvider();
        getLogger().info("Economy plugin connected to Vault: " + economy.getName());
    }

    private SchedulerService createSchedulerService() {
        try {
            Class.forName("io.papermc.paper.threadedregions.scheduler.ScheduledTask");
            getLogger().info("Scheduler active: Folia");
            return new FoliaSchedulerService(this);
        } catch (ClassNotFoundException exception) {
            getLogger().info("Scheduler active: Bukkit/Paper classic");
            return new SpigotSchedulerService(this);
        }
    }

    private TeleporterService createTeleporterService() {
        try {
            Entity.class.getMethod("teleportAsync", Location.class, PlayerTeleportEvent.TeleportCause.class);
            getLogger().info("Teleporter active: Paper async");
            return new PaperTeleporterService(schedulerService);
        } catch (NoSuchMethodException exception) {
            getLogger().info("Teleporter active: Bukkit sync");
            return new SpigotTeleporterService(schedulerService);
        }
    }

    private void registerPlaceholderApi() {
        if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            getLogger().info("PlaceholderAPI not found. External placeholders disabled.");
            return;
        }

        placeholderExpansion = new NexusBeaconPlaceholderExpansion(this);
        placeholderExpansion.register();

        getLogger().info("PlaceholderAPI successfully registered.");
    }
}