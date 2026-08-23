package cl.dynasty.nexusbeacon.bootstrap;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.RegisteredServiceProvider;

import cl.dynasty.nexusbeacon.NexusBeaconPlugin;
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
import cl.dynasty.nexusbeacon.platform.PlatformDescriptor;
import cl.dynasty.nexusbeacon.platform.PlatformEnvironment;
import cl.dynasty.nexusbeacon.platform.api.PlatformServices;
import cl.dynasty.nexusbeacon.platform.api.ScheduledTaskHandle;
import cl.dynasty.nexusbeacon.platform.api.SchedulerService;
import cl.dynasty.nexusbeacon.platform.api.TeleporterService;
import cl.dynasty.nexusbeacon.platform.modern.ModernPlatformFactory;
import cl.dynasty.nexusbeacon.platform.modern.ModernVersionSelection;
import cl.dynasty.nexusbeacon.platform.modern.ModernVersionSelector;
import cl.dynasty.nexusbeacon.service.ParticleService;
import cl.dynasty.nexusbeacon.storage.StorageManager;
import cl.dynasty.nexusbeacon.task.BeaconParticleTask;
import cl.dynasty.nexusbeacon.task.BeaconTickTask;
import cl.dynasty.nexusbeacon.task.BeaconVisualBeamTask;
import net.milkbowl.vault.economy.Economy;

public final class ModernPluginBootstrap {
    private final NexusBeaconPlugin plugin;

    private PlatformDescriptor platformDescriptor;
    private ModernVersionSelection modernVersionSelection;
    private VersionAdapter versionAdapter;
    private ConfigManager configManager;
    private LanguageManager languageManager;
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
    private SchedulerService schedulerService;
    private ScheduledTaskHandle beaconTickHandle;
    private ScheduledTaskHandle beaconParticleHandle;
    private TeleporterService teleporterService;
    private ParticleService particleService;
    private NexusBeaconPlaceholderExpansion placeholderExpansion;
    private BeamStyleManager beamStyleManager;
    private ScheduledTaskHandle visualBeamHandle;
    private boolean enabled;

    public ModernPluginBootstrap(NexusBeaconPlugin plugin) {
        this.plugin = plugin;
    }

    public void enable() {
        if (enabled) throw new IllegalStateException("NexusBeacon bootstrap is already enabled");

        printStartupBanner();
        platformDescriptor = PlatformEnvironment.detect(plugin.getServer());
        modernVersionSelection = ModernVersionSelector.select(
                platformDescriptor.getMinecraftVersion(), platformDescriptor.getJavaFeatureVersion());

        PlatformServices platformServices = ModernPlatformFactory.create(plugin);
        schedulerService = platformServices.getScheduler();
        teleporterService = platformServices.getTeleporter();
        particleService = new ParticleService();
        versionAdapter = new ModernAdapter();

        configManager = new ConfigManager(plugin);
        configManager.load();

        languageManager = new LanguageManager(plugin);
        languageManager.load();

        beamStyleManager = new BeamStyleManager(plugin);
        beamStyleManager.load();

        storageManager = new StorageManager(plugin);

        effectRegistry = new EffectRegistry(plugin);
        effectExecutorRegistry = new EffectExecutorRegistry(plugin);

        beaconManager = new BeaconManager(plugin);
        playerSettingsManager = new PlayerSettingsManager(plugin);
        beaconGuiManager = new BeaconGuiManager(plugin);
        customBeaconItemManager = new CustomBeaconItemManager(plugin);
        customRecipeManager = new CustomRecipeManager(plugin);
        beaconPowerManager = new BeaconPowerManager(plugin);

        if (ModernItemDataAdapter.isSupported()) {
            itemDataAdapter = new ModernItemDataAdapter(plugin);
        } else {
            plugin.getLogger().severe(languageManager.get("console.adapter.persistent-data-unavailable"));
            plugin.getServer().getPluginManager().disablePlugin(plugin);
            return;
        }

        setupEconomy();
        paymentManager = new PaymentManager(plugin);

        spawnerMetaHook = new SpawnerMetaHook();
        spawnerMetaHook.load();
        if (spawnerMetaHook.isEnabled()) {
            plugin.getLogger().info("SpawnerMeta detected. Spawners hook activated.");
        }

        reloadAll();
        restartRuntimeTasks();
        registerPlaceholderApi();
        customRecipeManager.load();

        NexusBeaconCommand command = new NexusBeaconCommand(plugin);
        PluginCommand nexusCommand = plugin.getCommand("NexusBeacon");
        if (nexusCommand == null) {
            plugin.getLogger().severe("Command 'NexusBeacon' is missing in plugin.yml. Disabling plugin.");
            plugin.getServer().getPluginManager().disablePlugin(plugin);
            return;
        }

        nexusCommand.setExecutor(command);
        nexusCommand.setTabCompleter(command);

        plugin.getServer().getPluginManager().registerEvents(new FurnaceBoostListener(plugin), plugin);
        Bukkit.getPluginManager().registerEvents(new BeaconListener(plugin), plugin);
        Bukkit.getPluginManager().registerEvents(new BeaconGuiListener(plugin), plugin);
        Bukkit.getPluginManager().registerEvents(new VanillaBeaconListener(plugin), plugin);
        Bukkit.getPluginManager().registerEvents(new NexusGuiListener(), plugin);

        enabled = true;
        plugin.getLogger().info("NexusBeacon successfully activated.");
        plugin.getLogger().info("Bukkit version detected: " + Bukkit.getBukkitVersion());
        plugin.getLogger().info("Active adapter: ModernAdapter");
    }

    public void disable() {
        cancel(beaconTickHandle);
        cancel(beaconParticleHandle);
        cancel(visualBeamHandle);

        if (placeholderExpansion != null) placeholderExpansion.unregister();
        if (storageManager != null) storageManager.close();

        enabled = false;
        plugin.getLogger().info("NexusBeacon successfully disabled.");
    }

    public void reloadAll() {
        configManager.load();
        languageManager.load();
        beamStyleManager.load();
        effectRegistry.load();
        effectExecutorRegistry.load();
        beaconManager.load();
    }

    public void restartRuntimeTasks() {
        restartVisualBeamTask();
        cancel(beaconTickHandle);
        cancel(beaconParticleHandle);

        int interval = configManager.getBeaconConfig().getInt("beacon.tick-interval", 40);
        int particleInterval = configManager.getBeaconConfig().getInt("beacon.particles.interval-ticks", 20);
        beaconTickHandle = schedulerService.runSyncTimer(new BeaconTickTask(plugin), interval, interval);
        beaconParticleHandle = schedulerService.runSyncTimer(
                new BeaconParticleTask(plugin), particleInterval, particleInterval);
    }

    public void restartVisualBeamTask() {
        cancel(visualBeamHandle);
        int interval = configManager.getBeaconConfig().getInt("visual-beam.interval-ticks", 4);
        visualBeamHandle = schedulerService.runSyncTimer(new BeaconVisualBeamTask(plugin), interval, interval);
    }

    private void cancel(ScheduledTaskHandle handle) {
        if (handle != null) handle.cancel();
    }

    private void printStartupBanner() {
        plugin.getLogger().info("_______________________________________________________");
        plugin.getLogger().info("");
        plugin.getLogger().info(" ███╗   ██╗███████╗██╗  ██╗██╗   ██╗███████╗");
        plugin.getLogger().info(" ████╗  ██║██╔════╝╚██╗██╔╝██║   ██║██╔════╝");
        plugin.getLogger().info(" ██╔██╗ ██║█████╗   ╚███╔╝ ██║   ██║███████╗");
        plugin.getLogger().info(" ██║╚██╗██║██╔══╝   ██╔██╗ ██║   ██║╚════██║");
        plugin.getLogger().info(" ██║ ╚████║███████╗██╔╝ ██╗╚██████╔╝███████║");
        plugin.getLogger().info(" ╚═╝  ╚═══╝╚══════╝╚═╝  ╚═╝ ╚═════╝ ╚══════╝");
        plugin.getLogger().info("");
        plugin.getLogger().info(" NexusBeacon v" + plugin.getDescription().getVersion());
        plugin.getLogger().info(" Running on " + plugin.getServer().getName() + " - " + plugin.getServer().getVersion());
        plugin.getLogger().info("_______________________________________________________");
    }

    private void setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().warning("Vault is not installed/loaded. Money payments disabled.");
            return;
        }
        if (!Bukkit.getPluginManager().isPluginEnabled("Vault")) {
            plugin.getLogger().warning("Vault is installed, but not yet enabled.");
            return;
        }
        RegisteredServiceProvider<Economy> provider = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (provider == null) {
            plugin.getLogger().warning("Vault is active, but there is no economy plugin registered with Vault.");
            return;
        }
        economy = provider.getProvider();
        plugin.getLogger().info("Economy plugin connected to Vault: " + economy.getName());
    }

    private void registerPlaceholderApi() {
        if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            plugin.getLogger().info("PlaceholderAPI not found. External placeholders disabled.");
            return;
        }
        placeholderExpansion = new NexusBeaconPlaceholderExpansion(plugin);
        placeholderExpansion.register();
        plugin.getLogger().info("PlaceholderAPI successfully registered.");
    }

    public PlatformDescriptor getPlatformDescriptor() { return platformDescriptor; }
    public ModernVersionSelection getModernVersionSelection() { return modernVersionSelection; }
    public VersionAdapter getVersionAdapter() { return versionAdapter; }
    public ConfigManager getConfigManager() { return configManager; }
    public LanguageManager getLanguageManager() { return languageManager; }
    public StorageManager getStorageManager() { return storageManager; }
    public BeaconManager getBeaconManager() { return beaconManager; }
    public PlayerSettingsManager getPlayerSettingsManager() { return playerSettingsManager; }
    public EffectRegistry getEffectRegistry() { return effectRegistry; }
    public BeaconGuiManager getBeaconGuiManager() { return beaconGuiManager; }
    public CustomBeaconItemManager getCustomBeaconItemManager() { return customBeaconItemManager; }
    public BeaconPowerManager getBeaconPowerManager() { return beaconPowerManager; }
    public PaymentManager getPaymentManager() { return paymentManager; }
    public ItemDataAdapter getItemDataAdapter() { return itemDataAdapter; }
    public Economy getEconomy() { return economy; }
    public CustomRecipeManager getCustomRecipeManager() { return customRecipeManager; }
    public EffectExecutorRegistry getEffectExecutorRegistry() { return effectExecutorRegistry; }
    public SpawnerMetaHook getSpawnerMetaHook() { return spawnerMetaHook; }
    public SchedulerService getSchedulerService() { return schedulerService; }
    public TeleporterService getTeleporterService() { return teleporterService; }
    public ParticleService getParticleService() { return particleService; }
    public BeamStyleManager getBeamStyleManager() { return beamStyleManager; }
}
