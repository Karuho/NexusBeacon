package cl.dynasty.nexusbeacon.platform.legacy;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import cl.dynasty.nexusbeacon.platform.api.PlatformServices;

/** Explicit, prepared Legacy application graph. Gameplay components are intentionally absent. */
public final class LegacyApplicationGraph {
    private final LegacyApplicationConfiguration configuration;
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
    private final EnumMap<LegacyApplicationCapability, LegacyCapabilityStatus> capabilityStates;
    private final Map<LegacyApplicationCapability, LegacyCapabilityStatus> capabilities;

    LegacyApplicationGraph(LegacyApplicationConfiguration configuration, PlatformServices platformServices,
            LegacyItemIdentityService identities, LegacyMaterialResolver materials,
            LegacyPotionEffectResolver potions, LegacyGuiItemFactory guiItems,
            LegacyInventoryFactory inventories, LegacyMessageService messages,
            LegacyParticleService particles, LegacyBeamCompatibility beams, LegacyApplicationState state) {
        this.configuration = nonNull(configuration, "configuration");
        this.platformServices = nonNull(platformServices, "platformServices");
        this.identities = nonNull(identities, "identities");
        this.materials = nonNull(materials, "materials");
        this.potions = nonNull(potions, "potions");
        this.guiItems = nonNull(guiItems, "guiItems");
        this.inventories = nonNull(inventories, "inventories");
        this.messages = nonNull(messages, "messages");
        this.particles = nonNull(particles, "particles");
        this.beams = nonNull(beams, "beams");
        this.state = nonNull(state, "state");
        if (!state.getStatus().isReady()) {
            throw new IllegalArgumentException("Legacy application state must be ready");
        }
        EnumMap<LegacyApplicationCapability, LegacyCapabilityStatus> states =
                new EnumMap<LegacyApplicationCapability, LegacyCapabilityStatus>(LegacyApplicationCapability.class);
        available(states, LegacyApplicationCapability.IDENTITY, LegacyApplicationCapability.MATERIAL,
                LegacyApplicationCapability.POTION, LegacyApplicationCapability.SCHEDULER,
                LegacyApplicationCapability.TELEPORT, LegacyApplicationCapability.GUI,
                LegacyApplicationCapability.MESSAGING, LegacyApplicationCapability.PARTICLES,
                LegacyApplicationCapability.BEAM, LegacyApplicationCapability.CONFIGURATION,
                LegacyApplicationCapability.APPLICATION_GRAPH, LegacyApplicationCapability.STORAGE);
        states.put(LegacyApplicationCapability.RECIPES, LegacyCapabilityStatus.DEFERRED);
        states.put(LegacyApplicationCapability.CROP_EFFECTS, LegacyCapabilityStatus.DEFERRED);
        states.put(LegacyApplicationCapability.LISTENERS, LegacyCapabilityStatus.NOT_WIRED);
        states.put(LegacyApplicationCapability.COMMANDS, LegacyCapabilityStatus.NOT_WIRED);
        states.put(LegacyApplicationCapability.INTEGRATIONS, LegacyCapabilityStatus.OPTIONAL);
        this.capabilityStates = states;
        this.capabilities = Collections.unmodifiableMap(states);
    }

    public LegacyApplicationConfiguration getConfiguration() { return configuration; }
    public PlatformServices getPlatformServices() { return platformServices; }
    public LegacyItemIdentityService getIdentities() { return identities; }
    public LegacyMaterialResolver getMaterials() { return materials; }
    public LegacyPotionEffectResolver getPotions() { return potions; }
    public LegacyGuiItemFactory getGuiItems() { return guiItems; }
    public LegacyInventoryFactory getInventories() { return inventories; }
    public LegacyMessageService getMessages() { return messages; }
    public LegacyParticleService getParticles() { return particles; }
    public LegacyBeamCompatibility getBeams() { return beams; }
    public LegacyApplicationState getState() { return state; }
    public Map<LegacyApplicationCapability, LegacyCapabilityStatus> getCapabilities() { return capabilities; }

    public LegacyCapabilityStatus getCapability(LegacyApplicationCapability capability) {
        LegacyCapabilityStatus status = capabilities.get(capability);
        if (status == null) throw new IllegalArgumentException("Unknown Legacy capability: " + capability);
        return status;
    }

    public void requireAvailable(LegacyApplicationCapability capability) {
        LegacyCapabilityStatus status = getCapability(capability);
        if (!status.isAvailable()) {
            throw new IllegalStateException(capability + " is " + status + " on the Legacy application path");
        }
    }

    public void setRecipeAvailable(boolean available) {
        capabilityStates.put(LegacyApplicationCapability.RECIPES,
                available ? LegacyCapabilityStatus.AVAILABLE : LegacyCapabilityStatus.UNAVAILABLE);
    }

    public void setCropEffectsAvailable(boolean available) {
        capabilityStates.put(LegacyApplicationCapability.CROP_EFFECTS,
                available ? LegacyCapabilityStatus.AVAILABLE : LegacyCapabilityStatus.UNAVAILABLE);
    }

    public void setListenersAvailable(boolean available) {
        capabilityStates.put(LegacyApplicationCapability.LISTENERS,
                available ? LegacyCapabilityStatus.AVAILABLE : LegacyCapabilityStatus.UNAVAILABLE);
    }

    public void setCommandsAvailable(boolean available) {
        capabilityStates.put(LegacyApplicationCapability.COMMANDS,
                available ? LegacyCapabilityStatus.AVAILABLE : LegacyCapabilityStatus.UNAVAILABLE);
    }

    private static void available(EnumMap<LegacyApplicationCapability, LegacyCapabilityStatus> states,
            LegacyApplicationCapability... capabilities) {
        for (LegacyApplicationCapability capability : capabilities) {
            states.put(capability, LegacyCapabilityStatus.AVAILABLE);
        }
    }

    private static <T> T nonNull(T value, String name) {
        if (value == null) throw new NullPointerException(name);
        return value;
    }
}
