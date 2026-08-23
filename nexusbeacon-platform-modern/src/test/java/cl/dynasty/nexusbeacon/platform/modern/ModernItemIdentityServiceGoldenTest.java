package cl.dynasty.nexusbeacon.platform.modern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Function;
import java.lang.reflect.Field;

import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import cl.dynasty.nexusbeacon.platform.api.IdentificationResult;
import cl.dynasty.nexusbeacon.platform.api.ItemIdentity;

class ModernItemIdentityServiceGoldenTest {

    private static ItemStack newItem() {
        try {
            Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            sun.misc.Unsafe unsafe = (sun.misc.Unsafe) field.get(null);
            return (ItemStack) unsafe.allocateInstance(ItemStack.class);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Could not allocate server-free ItemStack test token", exception);
        }
    }

    @Test
    void newlyMarkedBeaconUsesTheExistingMarkerAndIsRecognized() {
        Fixture fixture = new Fixture();
        ItemStack item = newItem();

        assertSame(item, fixture.service.mark(item, ItemIdentity.NEXUS_BEACON));
        assertEquals("true", fixture.markers.get(item));

        IdentificationResult result = fixture.service.identify(item);
        assertTrue(result.isRecognized());
        assertEquals(ItemIdentity.NEXUS_BEACON, result.getIdentity().orElseThrow());
        assertEquals(IdentificationResult.Evidence.PERSISTENT_MARKER, result.getEvidence());
    }

    @Test
    void ordinaryVanillaItemsWithoutTheConfiguredNameAreNotRecognized() {
        Fixture fixture = new Fixture();
        assertFalse(fixture.service.identify(newItem()).isRecognized());
        assertFalse(fixture.service.identify(newItem()).isRecognized());
    }

    @Test
    void visuallyIdenticalVanillaBeaconIsRecognizedByHistoricalFallback() {
        Fixture fixture = new Fixture();
        ItemStack lookalike = newItem();
        fixture.legacyMatches.put(lookalike, true);

        IdentificationResult result = fixture.service.identify(lookalike);
        assertTrue(result.isRecognized());
        assertEquals(IdentificationResult.Evidence.LEGACY_DISPLAY_NAME, result.getEvidence());
    }

    @Test
    void displayAndLoreChangesDoNotOverridePersistentIdentity() {
        Fixture fixture = new Fixture();
        ItemStack renamedOrRelored = newItem();
        fixture.markers.put(renamedOrRelored, "true");

        IdentificationResult result = fixture.service.identify(renamedOrRelored);
        assertTrue(result.isRecognized());
        assertEquals(IdentificationResult.Evidence.PERSISTENT_MARKER, result.getEvidence());
    }

    @Test
    void malformedMarkerIsExplicitAndIsNotSilentlyRepaired() {
        Fixture fixture = new Fixture();
        ItemStack item = newItem();
        fixture.markers.put(item, "yes");

        IdentificationResult result = fixture.service.identify(item);
        assertEquals(IdentificationResult.Status.MALFORMED_METADATA, result.getStatus());
        assertFalse(result.isRecognized());
        assertEquals("yes", fixture.markers.get(item));
    }

    @Test
    void malformedMarkerStillUsesThePreexistingDisplayNameFallback() {
        Fixture fixture = new Fixture();
        ItemStack item = newItem();
        fixture.markers.put(item, "yes");
        fixture.legacyMatches.put(item, true);

        IdentificationResult result = fixture.service.identify(item);
        assertTrue(result.isRecognized());
        assertEquals(IdentificationResult.Evidence.LEGACY_DISPLAY_NAME, result.getEvidence());
        assertEquals("yes", fixture.markers.get(item));
    }

    @Test
    void missingMarkerUsesOnlyTheExistingHistoricalFallback() {
        Fixture fixture = new Fixture();
        ItemStack item = newItem();
        assertEquals(IdentificationResult.Status.NOT_RECOGNIZED, fixture.service.identify(item).getStatus());

        fixture.legacyMatches.put(item, true);
        assertTrue(fixture.service.identify(item).isRecognized());
    }

    @Test
    void clonedItemRemainsRecognizedWhenPhysicalStoragePreservesMarker() {
        Fixture fixture = new Fixture();
        ItemStack original = newItem();
        fixture.service.mark(original, ItemIdentity.NEXUS_BEACON);

        ItemStack clone = newItem();
        fixture.copyPhysicalMetadata(original, clone);

        assertTrue(fixture.service.identify(clone).isRecognized());
        assertEquals("true", fixture.markers.get(clone));
    }

    @Test
    void recipeResultUsesTheSameCanonicalIdentity() {
        Fixture fixture = new Fixture();
        ItemStack recipeResult = newItem();
        fixture.service.mark(recipeResult, ItemIdentity.NEXUS_BEACON);

        IdentificationResult result = fixture.service.identify(recipeResult);
        assertEquals(ItemIdentity.NEXUS_BEACON, result.getIdentity().orElseThrow());
        assertEquals(IdentificationResult.Evidence.PERSISTENT_MARKER, result.getEvidence());
    }

    private static final class Fixture {
        private final Map<ItemStack, String> markers = new IdentityHashMap<>();
        private final Map<ItemStack, Boolean> legacyMatches = new IdentityHashMap<>();
        private final ModernItemIdentityService service = new ModernItemIdentityService(
                markers::get,
                writeMarker(),
                item -> legacyMatches.getOrDefault(item, false));

        private Function<ItemStack, ItemStack> writeMarker() {
            return item -> {
                markers.put(item, "true");
                return item;
            };
        }

        private void copyPhysicalMetadata(ItemStack source, ItemStack target) {
            markers.put(target, markers.get(source));
        }
    }
}
