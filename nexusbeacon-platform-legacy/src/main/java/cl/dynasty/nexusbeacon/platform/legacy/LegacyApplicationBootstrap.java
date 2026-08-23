package cl.dynasty.nexusbeacon.platform.legacy;

import org.bukkit.configuration.file.FileConfiguration;

import cl.dynasty.nexusbeacon.platform.api.PlatformServices;

/** Builds the application-preparation graph without activating gameplay. */
public final class LegacyApplicationBootstrap {
    private final PlatformServices platformServices;
    private final LegacyItemIdentityService identities;
    private final LegacyMaterialResolver materials;
    private final LegacyPotionEffectResolver potions;
    private final LegacyGuiItemFactory guiItems;
    private final LegacyInventoryFactory inventories;
    private final LegacyMessageService messages;
    private final LegacyParticleService particles;
    private final LegacyBeamCompatibility beams;
    private final LegacyApplicationState state;

    public LegacyApplicationBootstrap(PlatformServices platformServices, LegacyItemIdentityService identities,
            LegacyMaterialResolver materials, LegacyPotionEffectResolver potions,
            LegacyGuiItemFactory guiItems, LegacyInventoryFactory inventories,
            LegacyMessageService messages, LegacyParticleService particles, LegacyBeamCompatibility beams,
            LegacyApplicationState state) {
        this.platformServices = platformServices;
        this.identities = identities;
        this.materials = materials;
        this.potions = potions;
        this.guiItems = guiItems;
        this.inventories = inventories;
        this.messages = messages;
        this.particles = particles;
        this.beams = beams;
        this.state = state;
    }

    public LegacyApplicationGraph prepare(FileConfiguration config, FileConfiguration beacon,
            FileConfiguration effects, FileConfiguration gui) {
        LegacyApplicationConfiguration configuration = new LegacyApplicationConfigurationValidator(
                materials, potions, particles, beams).validate(config, beacon, effects, gui);
        if (state == null) throw new IllegalStateException("Legacy storage/application state is not configured");
        return new LegacyApplicationGraph(configuration, platformServices, identities, materials, potions,
                guiItems, inventories, messages, particles, beams, state);
    }
}
