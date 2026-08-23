package cl.dynasty.nexusbeacon.platform.legacy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Collections;

import cl.dynasty.nexusbeacon.platform.api.PlatformServices;
import cl.dynasty.nexusbeacon.platform.api.TeleporterService;

class LegacyApplicationGraphTest {
    @Test void exposesRealPlatformServicesAndFailClosedDeferredSubsystems() {
        Components components = components();
        LegacyApplicationGraph graph = components.graph;

        assertSame(components.platform, graph.getPlatformServices());
        assertSame(components.materials, graph.getMaterials());
        assertSame(components.particles, graph.getParticles());
        assertEquals(LegacyCapabilityStatus.AVAILABLE,
                graph.getCapability(LegacyApplicationCapability.APPLICATION_GRAPH));
        assertEquals(LegacyCapabilityStatus.AVAILABLE,
                graph.getCapability(LegacyApplicationCapability.STORAGE));
        assertEquals(LegacyCapabilityStatus.DEFERRED,
                graph.getCapability(LegacyApplicationCapability.RECIPES));
        assertThrows(IllegalStateException.class,
                () -> graph.requireAvailable(LegacyApplicationCapability.RECIPES));
        assertThrows(IllegalStateException.class,
                () -> graph.requireAvailable(LegacyApplicationCapability.CROP_EFFECTS));
    }

    @Test void graphContainsNoModernImplementationType() {
        LegacyApplicationGraph graph = components().graph;

        for (Object component : new Object[] { graph.getPlatformServices(), graph.getIdentities(),
                graph.getMaterials(), graph.getPotions(), graph.getGuiItems(), graph.getInventories(),
                graph.getMessages(), graph.getParticles(), graph.getBeams(), graph.getState() }) {
            assertFalse(component.getClass().getName().contains(".modern."), component.getClass().getName());
        }
    }

    @Test void recipeCapabilityReflectsProductiveRegistrationOutcome() {
        LegacyApplicationGraph graph = components().graph;
        graph.setRecipeAvailable(true);
        assertEquals(LegacyCapabilityStatus.AVAILABLE,
                graph.getCapability(LegacyApplicationCapability.RECIPES));
        graph.requireAvailable(LegacyApplicationCapability.RECIPES);

        graph.setRecipeAvailable(false);
        assertEquals(LegacyCapabilityStatus.UNAVAILABLE,
                graph.getCapability(LegacyApplicationCapability.RECIPES));
        assertThrows(IllegalStateException.class,
                () -> graph.requireAvailable(LegacyApplicationCapability.RECIPES));
    }

    @Test void effectCapabilityOnlyBecomesAvailableAfterProductiveRuntimeWiring() {
        LegacyApplicationGraph graph = components().graph;
        graph.setCropEffectsAvailable(true);
        assertEquals(LegacyCapabilityStatus.AVAILABLE,
                graph.getCapability(LegacyApplicationCapability.CROP_EFFECTS));
        graph.requireAvailable(LegacyApplicationCapability.CROP_EFFECTS);

        graph.setCropEffectsAvailable(false);
        assertEquals(LegacyCapabilityStatus.UNAVAILABLE,
                graph.getCapability(LegacyApplicationCapability.CROP_EFFECTS));
    }

    private static Components components() {
        Components result = new Components();
        ImmediateScheduler scheduler = new ImmediateScheduler();
        TeleporterService teleporter = (entity, location, cause) -> { };
        result.platform = new PlatformServices(scheduler, teleporter);
        result.materials = new LegacyMaterialResolver();
        LegacyPotionEffectResolver potions = new LegacyPotionEffectResolver();
        LegacyTextFormatter text = new LegacyTextFormatter();
        LegacyItemIdentityService identities = new LegacyItemIdentityService(new LegacyNbtBridge() {
            @Override public ItemStack mark(ItemStack item) { return item; }
            @Override public LegacyIdentityStatus identify(ItemStack item) {
                return LegacyIdentityStatus.NOT_RECOGNIZED;
            }
            @Override public String getRevision() { return "test"; }
        });
        LegacyGuiItemFactory guiItems = new LegacyGuiItemFactory(result.materials, text);
        LegacyInventoryFactory inventories = new LegacyInventoryFactory();
        LegacyMessageService messages = new LegacyMessageService(text);
        result.particles = new LegacyParticleService(
                LegacyParticleRuntime.SPIGOT_1_8, scheduler, new RecordingParticleTransport());
        LegacyBeamCompatibility beams = new LegacyBeamCompatibility(result.particles);
        LegacyApplicationConfiguration config = new LegacyApplicationConfiguration(
                "en_us", "YAML", "FIXED", 2, 1, 1, 0, 1, 0, 1, true);
        LegacyApplicationState state = new LegacyApplicationState(new LegacyBeaconStorage() {
            @Override public LegacyStorageLoadResult load() {
                return LegacyStorageLoadResult.success(Collections.<LegacyBeaconState>emptyList());
            }
            @Override public void store(Collection<LegacyBeaconState> beacons) { }
            @Override public void close() { }
            @Override public String getBackendName() { return "TEST"; }
        });
        state.initialize();
        result.graph = new LegacyApplicationGraph(config, result.platform, identities, result.materials,
                potions, guiItems, inventories, messages, result.particles, beams, state);
        return result;
    }

    private static final class Components {
        private PlatformServices platform;
        private LegacyMaterialResolver materials;
        private LegacyParticleService particles;
        private LegacyApplicationGraph graph;
    }
}
