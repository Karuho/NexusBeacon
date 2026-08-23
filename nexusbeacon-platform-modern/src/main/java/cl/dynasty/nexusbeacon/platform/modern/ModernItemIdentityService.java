package cl.dynasty.nexusbeacon.platform.modern;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

import org.bukkit.inventory.ItemStack;

import cl.dynasty.nexusbeacon.platform.api.IdentificationResult;
import cl.dynasty.nexusbeacon.platform.api.ItemIdentity;
import cl.dynasty.nexusbeacon.platform.api.ItemIdentityService;

public final class ModernItemIdentityService implements ItemIdentityService {

    private final Function<ItemStack, String> markerReader;
    private final Function<ItemStack, ItemStack> markerWriter;
    private final Predicate<ItemStack> legacyDisplayMatcher;

    public ModernItemIdentityService(
            Function<ItemStack, String> markerReader,
            Function<ItemStack, ItemStack> markerWriter,
            Predicate<ItemStack> legacyDisplayMatcher) {
        this.markerReader = Objects.requireNonNull(markerReader, "markerReader");
        this.markerWriter = Objects.requireNonNull(markerWriter, "markerWriter");
        this.legacyDisplayMatcher = Objects.requireNonNull(legacyDisplayMatcher, "legacyDisplayMatcher");
    }

    @Override
    public IdentificationResult identify(ItemStack item) {
        if (item == null) {
            return IdentificationResult.notRecognized();
        }

        String marker = markerReader.apply(item);
        if (marker != null) {
            if ("true".equalsIgnoreCase(marker)) {
                return IdentificationResult.recognized(
                        ItemIdentity.NEXUS_BEACON,
                        IdentificationResult.Evidence.PERSISTENT_MARKER);
            }
            if (legacyDisplayMatcher.test(item)) {
                return IdentificationResult.recognized(
                        ItemIdentity.NEXUS_BEACON,
                        IdentificationResult.Evidence.LEGACY_DISPLAY_NAME);
            }
            return IdentificationResult.malformed();
        }

        if (legacyDisplayMatcher.test(item)) {
            return IdentificationResult.recognized(
                    ItemIdentity.NEXUS_BEACON,
                    IdentificationResult.Evidence.LEGACY_DISPLAY_NAME);
        }

        return IdentificationResult.notRecognized();
    }

    @Override
    public ItemStack mark(ItemStack item, ItemIdentity identity) {
        Objects.requireNonNull(item, "item");
        Objects.requireNonNull(identity, "identity");
        if (identity != ItemIdentity.NEXUS_BEACON) {
            throw new IllegalArgumentException("Unsupported item identity: " + identity);
        }
        return markerWriter.apply(item);
    }
}
