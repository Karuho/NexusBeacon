package cl.dynasty.nexusbeacon.platform.legacy;

import java.util.Optional;
import org.bukkit.inventory.ItemStack;
import cl.dynasty.nexusbeacon.platform.api.IdentificationResult;
import cl.dynasty.nexusbeacon.platform.api.ItemIdentity;
import cl.dynasty.nexusbeacon.platform.api.ItemIdentityService;

public final class LegacyItemIdentityService implements ItemIdentityService {
    private final LegacyNbtBridge bridge;

    public LegacyItemIdentityService(LegacyNbtBridge bridge) { this.bridge = bridge; }

    @Override
    public IdentificationResult identify(ItemStack item) {
        LegacyIdentityStatus status = bridge.identify(item);
        if (status == LegacyIdentityStatus.RECOGNIZED) {
            return IdentificationResult.recognized(ItemIdentity.NEXUS_BEACON,
                    IdentificationResult.Evidence.PERSISTENT_MARKER);
        }
        return status == LegacyIdentityStatus.NOT_RECOGNIZED
                ? IdentificationResult.notRecognized()
                : IdentificationResult.malformed();
    }

    @Override
    public ItemStack mark(ItemStack item, ItemIdentity identity) {
        if (identity != ItemIdentity.NEXUS_BEACON) throw new IllegalArgumentException("Unsupported identity: " + identity);
        return bridge.mark(item);
    }

    public ItemStack markPortable(ItemStack item, LegacyPortableBeaconData data) {
        return bridge.writePortableData(mark(item, ItemIdentity.NEXUS_BEACON), data);
    }

    public Optional<LegacyPortableBeaconData> readPortableData(ItemStack item) {
        return bridge.readPortableData(item);
    }
}
