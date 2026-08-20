package cl.dynasty.nexusbeacon.legacy;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import cl.dynasty.nexusbeacon.platform.api.ItemIdentity;
import cl.dynasty.nexusbeacon.platform.legacy.LegacyItemIdentityService;
import cl.dynasty.nexusbeacon.platform.legacy.LegacyNbtBridge;
import cl.dynasty.nexusbeacon.platform.legacy.LegacyNbtBridgeFactory;
import cl.dynasty.nexusbeacon.platform.api.MaterialContext;
import cl.dynasty.nexusbeacon.platform.legacy.LegacyMaterialResolver;
import cl.dynasty.nexusbeacon.platform.legacy.LegacyMaterialResolution;
import cl.dynasty.nexusbeacon.platform.legacy.LegacyPotionEffectResolver;
import cl.dynasty.nexusbeacon.platform.api.PlatformServices;
import cl.dynasty.nexusbeacon.platform.legacy.LegacyPlatformFactory;
import cl.dynasty.nexusbeacon.platform.legacy.LegacyGuiItemFactory;
import cl.dynasty.nexusbeacon.platform.legacy.LegacyInventoryFactory;
import cl.dynasty.nexusbeacon.platform.legacy.LegacyMessageService;
import cl.dynasty.nexusbeacon.platform.legacy.LegacyTextFormatter;
import cl.dynasty.nexusbeacon.platform.legacy.LegacyBeamCompatibility;
import cl.dynasty.nexusbeacon.platform.legacy.LegacyBeamCompatibilityStatus;
import cl.dynasty.nexusbeacon.platform.legacy.LegacyBeamRenderer;
import cl.dynasty.nexusbeacon.platform.legacy.LegacyBeamStyleCompatibility;
import cl.dynasty.nexusbeacon.platform.legacy.LegacyBeamStylePlan;
import cl.dynasty.nexusbeacon.platform.legacy.LegacyParticleRequest;
import cl.dynasty.nexusbeacon.platform.legacy.LegacyParticleRuntime;
import cl.dynasty.nexusbeacon.platform.legacy.LegacyParticleService;
import cl.dynasty.nexusbeacon.platform.legacy.LegacyApplicationBootstrap;
import cl.dynasty.nexusbeacon.platform.legacy.LegacyApplicationConfiguration;
import cl.dynasty.nexusbeacon.platform.legacy.LegacyApplicationGraph;
import cl.dynasty.nexusbeacon.platform.legacy.LegacyApplicationCapability;
import cl.dynasty.nexusbeacon.platform.legacy.LegacyCapabilityStatus;
import cl.dynasty.nexusbeacon.platform.legacy.LegacyListenerGraph;
import cl.dynasty.nexusbeacon.platform.legacy.LegacyApplicationState;
import cl.dynasty.nexusbeacon.platform.legacy.LegacyStorageLoadResult;
import cl.dynasty.nexusbeacon.platform.legacy.LegacyStorageFactory;
import cl.dynasty.nexusbeacon.platform.legacy.LegacyBeaconGameplaySettings;
import cl.dynasty.nexusbeacon.platform.legacy.LegacyBeaconItemFactory;
import cl.dynasty.nexusbeacon.platform.legacy.LegacyBeaconListenerMessages;
import cl.dynasty.nexusbeacon.platform.legacy.LegacyRecipeManager;
import cl.dynasty.nexusbeacon.platform.legacy.LegacyRecipeRegistrationResult;
import cl.dynasty.nexusbeacon.platform.legacy.LegacyRecipeRegistrationStatus;
import cl.dynasty.nexusbeacon.platform.legacy.LegacyEffectRuntime;

public final class LegacyNexusBeaconPlugin extends JavaPlugin {
    private LegacyNbtBridge bridge;
    private PlatformServices platformServices;
    private LegacyApplicationGraph applicationGraph;
    private LegacyListenerGraph listenerGraph;
    private LegacyApplicationState applicationState;
    private LegacyBeaconItemFactory beaconItems;
    private LegacyRecipeManager recipes;
    private LegacyEffectRuntime effectRuntime;

    @Override
    public void onEnable() {
        String craftPackage = Bukkit.getServer().getClass().getPackage().getName();
        try {
            bridge = LegacyNbtBridgeFactory.create(craftPackage);
            LegacyItemIdentityService identities = new LegacyItemIdentityService(bridge);
            ItemStack marked = identities.mark(new ItemStack(Material.BEACON), ItemIdentity.NEXUS_BEACON);
            if (!identities.identify(marked).isRecognized()) {
                throw new IllegalStateException("Legacy NBT identity self-check failed");
            }
            LegacyMaterialResolver materials = new LegacyMaterialResolver();
            LegacyPotionEffectResolver potions = new LegacyPotionEffectResolver();
            if (!materials.resolveMaterial("BEACON", MaterialContext.REQUIRED_ITEM).isResolved()
                    || !materials.resolveMaterial("COBWEB", MaterialContext.EFFECT_ICON).isResolved()) {
                throw new IllegalStateException("Legacy material resolver self-check failed");
            }
            LegacyMaterialResolution pane = materials.resolveLegacyMaterial(
                    "BLACK_STAINED_GLASS_PANE", MaterialContext.GUI_ICON);
            if (!pane.getResolution().isResolved() || pane.getData() != 15
                    || materials.resolveMaterial("NETHERITE_SWORD", MaterialContext.REQUIRED_ITEM).isResolved()
                    || !materials.resolveMaterial("REINFORCED_DEEPSLATE", MaterialContext.GUI_ICON).isFallbackUsed()) {
                throw new IllegalStateException("Legacy material mapping self-check failed");
            }
            if (!potions.resolvePotionEffect("SPEED").isResolved()
                    || !potions.resolvePotionEffect("STRENGTH").isResolved()
                    || potions.resolvePotionEffect("DARKNESS").isResolved()) {
                throw new IllegalStateException("Legacy potion resolver self-check failed");
            }
            LegacyTextFormatter text = new LegacyTextFormatter();
            LegacyGuiItemFactory guiItems = new LegacyGuiItemFactory(materials, text);
            ItemStack paneItem = guiItems.createItem("BLACK_STAINED_GLASS_PANE", MaterialContext.GUI_ICON,
                    "&aLegacy GUI", java.util.Arrays.asList("&7Compatible"), Integer.valueOf(123));
            ItemMeta paneMeta = paneItem.getItemMeta();
            if (paneItem.getType() != Material.STAINED_GLASS_PANE || paneItem.getDurability() != 15
                    || paneMeta == null || !"\u00a7aLegacy GUI".equals(paneMeta.getDisplayName())) {
                throw new IllegalStateException("Legacy GUI item self-check failed");
            }
            ItemStack visualFallback = guiItems.createVisualItemWithFallback("REINFORCED_DEEPSLATE",
                    "&7Legacy visual", java.util.Collections.<String>emptyList(), null);
            if (visualFallback.getType() != Material.STONE) {
                throw new IllegalStateException("Legacy GUI visual fallback self-check failed");
            }
            ItemStack head = guiItems.createPlayerHead("NexusBeacon", "&aPlayer", java.util.Collections.<String>emptyList());
            if (head.getType() != Material.SKULL_ITEM || head.getDurability() != 3
                    || !(head.getItemMeta() instanceof SkullMeta)
                    || !"NexusBeacon".equals(((SkullMeta) head.getItemMeta()).getOwner())
                    || guiItems.supportsCustomTextureHeads()) {
                throw new IllegalStateException("Legacy player skull self-check failed");
            }
            LegacyInventoryFactory inventories = new LegacyInventoryFactory();
            Inventory inventory = inventories.create(new InventoryHolder() {
                @Override public Inventory getInventory() { return null; }
            }, 9, "\u00a78NexusBeacon");
            if (inventory.getSize() != 9 || !"\u00a78NexusBeacon".equals(inventory.getTitle())) {
                throw new IllegalStateException("Legacy inventory self-check failed");
            }
            LegacyMessageService messages = new LegacyMessageService(text);
            platformServices = LegacyPlatformFactory.create(this);
            LegacyParticleRuntime particleRuntime = LegacyParticleRuntime.fromCraftPackage(craftPackage);
            LegacyParticleService particles = new LegacyParticleService(
                    particleRuntime, platformServices.getScheduler());
            LegacyBeamCompatibility beamCompatibility = new LegacyBeamCompatibility(particles);
            LegacyBeamRenderer beamRenderer = new LegacyBeamRenderer(particles, platformServices.getScheduler());
            int fullBeamStyles = 0;
            int degradedBeamStyles = 0;
            for (LegacyBeamStylePlan style : LegacyBeamStylePlan.currentDefaults()) {
                LegacyBeamStyleCompatibility compatibility = beamCompatibility.classify(style);
                if (compatibility.getStatus() == LegacyBeamCompatibilityStatus.UNSUPPORTED) {
                    throw new IllegalStateException("Legacy beam style is unsupported: " + style.getId());
                }
                if (compatibility.getStatus() == LegacyBeamCompatibilityStatus.VISUAL_DEGRADATION) {
                    degradedBeamStyles++;
                } else {
                    fullBeamStyles++;
                }
            }
            if (fullBeamStyles + degradedBeamStyles != 5
                    || particles.resolve("SONIC_BOOM").isSupported()
                    || particles.resolve("not-a-particle").isSupported()) {
                throw new IllegalStateException("Legacy particle/beam compatibility self-check failed");
            }
            if (!Bukkit.getWorlds().isEmpty()) {
                World world = Bukkit.getWorlds().get(0);
                Location probe = world.getSpawnLocation().clone().add(0.5D, 1.0D, 0.5D);
                particles.emitToWorld(world, new LegacyParticleRequest(
                        "VILLAGER_HAPPY", probe, 1, 0.0D, 0.0D, 0.0D, 0.0D, null, 1.0F));
                particles.emitToWorld(world, new LegacyParticleRequest(
                        "DUST", probe, 1, 0.0D, 0.0D, 0.0D, 0.0D, null, 1.0F));
                beamRenderer.render(
                        probe, 1, 1.0D, 1, LegacyBeamStylePlan.currentDefaults().get(4));
            }
            saveApplicationResources();
            FileConfiguration mainConfig = loadConfiguration("config.yml");
            applicationState = new LegacyApplicationState(
                    LegacyStorageFactory.create(mainConfig.getString("storage.type"),
                            new File(getDataFolder(), "storage.yml")));
            LegacyStorageLoadResult storageLoad = applicationState.initialize();
            if (!storageLoad.isSuccessful()) {
                throw new IllegalStateException("Legacy storage " + storageLoad.getStatus().name().toLowerCase()
                        + ": " + storageLoad.getDiagnostic());
            }
            LegacyApplicationBootstrap applicationBootstrap = new LegacyApplicationBootstrap(
                    platformServices, identities, materials, potions, guiItems, inventories,
                    messages, particles, beamCompatibility, applicationState);
            FileConfiguration beaconConfig = loadConfiguration("beacon.yml");
            FileConfiguration effectsConfig = loadConfiguration("effects.yml");
            applicationGraph = applicationBootstrap.prepare(
                    mainConfig, beaconConfig, effectsConfig, loadConfiguration("gui.yml"));
            assertUnavailable(LegacyApplicationCapability.LISTENERS);
            assertUnavailable(LegacyApplicationCapability.COMMANDS);
            applicationGraph.requireAvailable(LegacyApplicationCapability.STORAGE);
            LegacyApplicationConfiguration applicationConfig = applicationGraph.getConfiguration();
            String languagePath = "languages/" + applicationConfig.getLanguage() + ".yml";
            saveResourceIfMissing(languagePath);
            FileConfiguration language = loadConfiguration(languagePath);
            String prefix = language.getString("prefix", "&b[NexusBeacon]&r ");
            String vanillaDisabledMessage = prefixed(language, prefix, "messages.beacon.vanilla-disabled",
                    "&cVanilla beacons are disabled.");
            LegacyBeaconGameplaySettings gameplaySettings = new LegacyBeaconGameplaySettings(
                    beaconConfig.getInt("beacon.default-range", 48),
                    beaconConfig.getBoolean("protection.protect-base-blocks", true),
                    beaconConfig.getBoolean("beacon.particles.enabled", true),
                    beaconConfig.getString("beacon.particles.type", "VILLAGER_HAPPY"),
                    beaconConfig.getBoolean("protection.owner-only-break", true),
                    beaconConfig.getBoolean("beacon-item.creative-no-dupe", true),
                    beaconConfig.getBoolean("beacon-item.cancel-if-inventory-full", true),
                    beaconConfig.getBoolean("beacon-item.auto-pickup", true));
            beaconItems = new LegacyBeaconItemFactory(identities, materials, text,
                    language.getString("item.display-name", "&b&lNexusBeacon"), freshItemLore(language));
            recipes = new LegacyRecipeManager(materials, beaconItems, identities);
            LegacyRecipeRegistrationResult recipeResult = recipes.register(
                    beaconConfig.getConfigurationSection("recipe"));
            applicationGraph.setRecipeAvailable(recipeResult.isAvailable());
            if (recipeResult.getStatus() == LegacyRecipeRegistrationStatus.REGISTERED) {
                getLogger().info(language.getString("messages.console.custom-recipe-registered",
                        "Custom NexusBeacon recipe registered."));
            } else if (recipeResult.getStatus() == LegacyRecipeRegistrationStatus.ALREADY_PRESENT) {
                getLogger().info("Custom NexusBeacon recipe already present; duplicate registration skipped.");
            } else if (recipeResult.getStatus() != LegacyRecipeRegistrationStatus.DISABLED) {
                getLogger().warning("Legacy recipe unavailable (" + recipeResult.getStatus() + "): "
                        + recipeResult.getDiagnostic());
            }
            effectRuntime = new LegacyEffectRuntime(this, applicationState, beaconConfig, effectsConfig,
                    materials, potions, platformServices.getScheduler());
            if (effectRuntime.getExecutorCount() != 7 || effectRuntime.getDefinitionCount() != 19) {
                throw new IllegalStateException("Legacy effect topology mismatch: "
                        + effectRuntime.getDefinitionCount() + " definitions, "
                        + effectRuntime.getExecutorCount() + " executors");
            }
            effectRuntime.start();
            applicationGraph.setCropEffectsAvailable(true);
            applicationGraph.requireAvailable(LegacyApplicationCapability.CROP_EFFECTS);
            LegacyBeaconListenerMessages listenerMessages = new LegacyBeaconListenerMessages(
                    prefixed(language, prefix, "messages.beacon.registered", "&aBeacon registered successfully."),
                    prefixed(language, prefix, "messages.beacon.removed", "&cBeacon removed."),
                    prefixed(language, prefix, "messages.beacon.place-no-permission",
                            "&cYou do not have permission to place this beacon."),
                    prefixed(language, prefix, "messages.beacon.break-not-owner",
                            "&cYou cannot break a NexusBeacon that does not belong to you."),
                    prefixed(language, prefix, "messages.beacon.inventory-full", "&cYour inventory is full."),
                    prefix + "&cThe NexusBeacon transaction failed safely.",
                    prefixed(language, prefix, "messages.beacon.already-registered",
                            "&eThis beacon is already registered."),
                    prefix + "&cThis NexusBeacon item has invalid identity data.");
            listenerGraph = new LegacyListenerGraph(this, applicationGraph,
                    beaconConfig.getBoolean("vanilla-beacon.disable-vanilla", false),
                    vanillaDisabledMessage, gameplaySettings, beaconItems, listenerMessages);
            if (!listenerGraph.register() || listenerGraph.register()) {
                throw new IllegalStateException("Legacy listener registration lifecycle self-check failed");
            }
            platformServices.getTeleporter().teleport(null, null, null);
            platformServices.getScheduler().runAsync(new Runnable() {
                @Override public void run() {
                    final boolean asynchronous = !Bukkit.isPrimaryThread();
                    platformServices.getScheduler().runSync(new Runnable() {
                        @Override public void run() {
                            if (asynchronous && Bukkit.isPrimaryThread()) {
                                getLogger().info("Legacy scheduler self-check passed.");
                            } else {
                                getLogger().severe("Legacy scheduler thread handoff self-check failed.");
                            }
                        }
                    });
                }
            });
            getLogger().info("Legacy platform foundation active: " + bridge.getRevision());
            getLogger().info("Legacy material and potion compatibility active.");
            getLogger().info("Legacy GUI, skull and message compatibility active.");
            getLogger().info("Legacy particle backend: Spigot Effect + "
                    + (particleRuntime.hasBukkitParticles() ? "Bukkit Particle" : "explicit visual fallback")
                    + " (" + particleRuntime.getRevision() + ")");
            getLogger().info("Legacy beam compatibility: 5 styles (" + fullBeamStyles
                    + " full, " + degradedBeamStyles + " visually degraded).");
            getLogger().info("Legacy scheduler active: Bukkit");
            getLogger().info("Legacy teleporter active: Bukkit sync");
            getLogger().info("Legacy application configuration validated: "
                    + applicationConfig.getConfiguredEffects() + " effects, "
                    + applicationConfig.getBeamStyles() + " beam styles, "
                    + applicationConfig.getGuiItems() + " GUI items.");
            getLogger().info("Legacy application core graph prepared.");
            getLogger().info("Storage backend: " + applicationState.getBackendName());
            getLogger().info("Application state: "
                    + (applicationState.getStatus().name().equals("READY_EMPTY") ? "empty" : "ready"));
            getLogger().info("Loaded beacons: " + applicationState.size());
            getLogger().info("Legacy listener graph registered: " + listenerGraph.getPortedListenerCount()
                    + " safe, " + listenerGraph.getDeferredListenerCount() + " deferred.");
            getLogger().info("Transactional marked beacon placement/removal active; interaction GUI deferred.");
            getLogger().info("Legacy effect runtime active: " + effectRuntime.getDefinitionCount()
                    + " definitions, " + effectRuntime.getExecutorCount() + " executors, "
                    + effectRuntime.getSupportedDefinitionCount() + " definitions supported.");
            getLogger().info("Legacy recipe status: " + recipeResult.getStatus() + "; commands: not wired.");
            getLogger().warning("Full Legacy gameplay startup is intentionally disabled.");
        } catch (RuntimeException exception) {
            getLogger().severe("Unsupported or failed Legacy platform: " + exception.getMessage());
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (effectRuntime != null) effectRuntime.close();
        if (recipes != null) {
            int removed = recipes.unregister();
            getLogger().info("Legacy NexusBeacon recipes removed: " + removed);
        }
        if (listenerGraph != null) {
            boolean dispatchObserved = listenerGraph.isEnableDispatchObserved();
            boolean unregistered = listenerGraph.unregister();
            if (unregistered && dispatchObserved) {
                getLogger().info("Legacy listeners unregistered cleanly after confirmed Bukkit dispatch.");
            } else {
                getLogger().warning("Legacy listener lifecycle ended without complete dispatch/unregister proof.");
            }
        }
        if (applicationState != null) applicationState.close();
        Bukkit.getScheduler().cancelTasks(this);
        if (bridge != null) getLogger().info("Legacy platform foundation disabled cleanly.");
    }

    private void saveApplicationResources() {
        saveDefaultConfig();
        saveResourceIfMissing("beacon.yml");
        saveResourceIfMissing("effects.yml");
        saveResourceIfMissing("gui.yml");
        saveResourceIfMissing("storage.yml");
    }

    private void saveResourceIfMissing(String path) {
        File file = new File(getDataFolder(), path);
        if (!file.exists()) saveResource(path, false);
    }

    private FileConfiguration loadConfiguration(String path) {
        File file = new File(getDataFolder(), path);
        FileConfiguration configuration = YamlConfiguration.loadConfiguration(file);
        if (configuration.getKeys(false).isEmpty()) {
            throw new IllegalStateException("Legacy application configuration is empty: " + path);
        }
        return configuration;
    }

    private String prefixed(FileConfiguration language, String prefix, String path, String fallback) {
        return prefix + language.getString(path, fallback);
    }

    private List<String> freshItemLore(FileConfiguration language) {
        List<String> result = new ArrayList<String>();
        String owner = language.getString("item.no-owner", "No owner");
        String trusted = language.getString("item.no-trusted", "No");
        String noEffects = language.getString("item.no-effects", "&7- &cNo acquired effects");
        for (String line : language.getStringList("item.lore")) {
            if ("{effect_list}".equalsIgnoreCase(line)) {
                result.add(noEffects);
            } else {
                result.add(line.replace("{owner}", owner).replace("{range}", "0")
                        .replace("{trusted}", trusted));
            }
        }
        if (result.isEmpty()) result.add(noEffects);
        return result;
    }

    private void assertUnavailable(LegacyApplicationCapability capability) {
        try {
            applicationGraph.requireAvailable(capability);
            throw new IllegalStateException(capability + " unexpectedly became available");
        } catch (IllegalStateException expected) {
            if (applicationGraph.getCapability(capability).isAvailable()) throw expected;
        }
    }

    public LegacyApplicationState getApplicationState() {
        if (applicationState == null || !applicationState.getStatus().isReady()) {
            throw new IllegalStateException("Legacy application state is not ready");
        }
        return applicationState;
    }

    public ItemStack createNexusBeaconItem(int amount) {
        if (beaconItems == null) throw new IllegalStateException("Legacy beacon item factory is not ready");
        return beaconItems.createNew(amount);
    }

    public LegacyEffectRuntime getEffectRuntime() {
        if (effectRuntime == null || !effectRuntime.isRunning()) {
            throw new IllegalStateException("Legacy effect runtime is not ready");
        }
        return effectRuntime;
    }
}
