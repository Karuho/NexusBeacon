package cl.dynasty.nexusbeacon.platform.classic;

import static org.junit.jupiter.api.Assertions.*;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import cl.dynasty.nexusbeacon.platform.api.IdentificationResult;
import cl.dynasty.nexusbeacon.platform.api.ItemIdentity;

class ClassicItemIdentityServiceTest {
    @Test void ordinaryAndSpoofedBeaconAreRejectedWithoutCanonicalMarker() {
        ClassicItemIdentityService service = service(null);
        assertFalse(service.identify(new ItemStack(Material.BEACON)).isRecognized());
    }
    @Test void canonicalMarkerIsRecognized() { assertTrue(service(Boolean.TRUE).identify(new ItemStack(Material.BEACON)).isRecognized()); }
    @Test void malformedMarkerFailsClosed() { assertEquals(IdentificationResult.Status.MALFORMED_METADATA, service(Boolean.FALSE).identify(new ItemStack(Material.BEACON)).getStatus()); }
    @Test void markingUsesCanonicalBackend() {
        final boolean[] written = {false}; ItemStack item = new ItemStack(Material.BEACON);
        ClassicItemIdentityService service = new ClassicItemIdentityService(new ClassicItemIdentityService.MarkerAccess() {
            public Boolean read(ItemStack ignored) { return null; }
            public ItemStack write(ItemStack input) { written[0] = true; return input.clone(); }
        });
        ItemStack marked = service.mark(item, ItemIdentity.NEXUS_BEACON);
        assertTrue(written[0]); assertNotSame(item, marked);
    }
    private static ClassicItemIdentityService service(final Boolean marker) {
        return new ClassicItemIdentityService(new ClassicItemIdentityService.MarkerAccess() {
            public Boolean read(ItemStack ignored) { return marker; }
            public ItemStack write(ItemStack input) { return input; }
        });
    }
}
