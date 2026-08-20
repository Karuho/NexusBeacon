package cl.dynasty.nexusbeacon.platform.legacy;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.inventory.ItemStack;

import cl.dynasty.nexusbeacon.platform.api.IdentificationResult;

/** Transactional state boundary for the intentionally narrow Legacy block subset. */
public final class LegacyBeaconTransactionService {
    private final LegacyApplicationState state;
    private final LegacyItemIdentityService identities;
    private final LegacyBeaconGameplaySettings settings;

    public LegacyBeaconTransactionService(LegacyApplicationState state,
            LegacyItemIdentityService identities, LegacyBeaconGameplaySettings settings) {
        if (state == null) throw new NullPointerException("state");
        if (identities == null) throw new NullPointerException("identities");
        if (settings == null) throw new NullPointerException("settings");
        this.state = state;
        this.identities = identities;
        this.settings = settings;
    }

    public LegacyBeaconTransactionResult place(ItemStack item, LegacyBeaconLocation location, UUID owner) {
        IdentificationResult identity = identities.identify(item);
        if (identity.getStatus() == IdentificationResult.Status.NOT_RECOGNIZED) {
            return LegacyBeaconTransactionResult.of(LegacyBeaconTransactionStatus.NOT_RECOGNIZED);
        }
        if (!identity.isRecognized()) {
            return LegacyBeaconTransactionResult.of(LegacyBeaconTransactionStatus.MALFORMED_ITEM);
        }
        try {
            if (state.find(location) != null) {
                return LegacyBeaconTransactionResult.of(LegacyBeaconTransactionStatus.DUPLICATE_LOCATION_OR_ID);
            }
            Optional<LegacyPortableBeaconData> portable = identities.readPortableData(item);
            UUID uniqueId = portable.isPresent() ? portable.get().getUniqueId() : UUID.randomUUID();
            LegacyBeaconState beacon = new LegacyBeaconState(location, uniqueId, owner,
                    settings.getDefaultRange(), 1,
                    portable.isPresent() ? portable.get().getEffectLevels()
                            : Collections.<String, Integer>emptyMap(),
                    portable.isPresent() ? portable.get().getActiveEffects()
                            : Collections.<String>emptySet(),
                    Collections.<UUID>emptySet(), settings.isProtectBaseBlocks(), null,
                    settings.isRangeParticlesEnabled(), settings.getRangeParticleType());
            if (!state.insert(beacon)) {
                return LegacyBeaconTransactionResult.of(LegacyBeaconTransactionStatus.DUPLICATE_LOCATION_OR_ID);
            }
            return LegacyBeaconTransactionResult.committed(beacon);
        } catch (IllegalArgumentException malformed) {
            return LegacyBeaconTransactionResult.failure(LegacyBeaconTransactionStatus.MALFORMED_ITEM,
                    malformed.getMessage());
        } catch (LegacyStorageException failure) {
            return LegacyBeaconTransactionResult.failure(LegacyBeaconTransactionStatus.STORAGE_FAILURE,
                    failure.getMessage());
        } catch (IllegalStateException unavailable) {
            return LegacyBeaconTransactionResult.failure(LegacyBeaconTransactionStatus.STORAGE_FAILURE,
                    unavailable.getMessage());
        }
    }

    public LegacyBeaconTransactionResult remove(LegacyBeaconLocation location,
            LegacyWorldBeaconMutation world) {
        if (world == null) throw new NullPointerException("world");
        LegacyBeaconState beacon;
        try {
            beacon = state.find(location);
        } catch (IllegalStateException unavailable) {
            return LegacyBeaconTransactionResult.failure(LegacyBeaconTransactionStatus.STORAGE_FAILURE,
                    unavailable.getMessage());
        }
        if (beacon == null) {
            return LegacyBeaconTransactionResult.of(LegacyBeaconTransactionStatus.UNMANAGED_BEACON);
        }
        if (!world.isBeacon()) {
            return LegacyBeaconTransactionResult.of(LegacyBeaconTransactionStatus.STATE_WORLD_MISMATCH);
        }
        try {
            if (!state.delete(location)) {
                return LegacyBeaconTransactionResult.of(LegacyBeaconTransactionStatus.UNMANAGED_BEACON);
            }
        } catch (RuntimeException failure) {
            return LegacyBeaconTransactionResult.failure(LegacyBeaconTransactionStatus.STORAGE_FAILURE,
                    failure.getMessage());
        }
        try {
            world.removeBeacon();
            return LegacyBeaconTransactionResult.committed(beacon);
        } catch (RuntimeException worldFailure) {
            try {
                if (!state.insert(beacon)) {
                    throw new LegacyStorageException("Removed state could not be restored");
                }
            } catch (RuntimeException recoveryFailure) {
                worldFailure.addSuppressed(recoveryFailure);
                throw new LegacyStorageException("World removal and authoritative recovery both failed", worldFailure);
            }
            return LegacyBeaconTransactionResult.failure(LegacyBeaconTransactionStatus.WORLD_MUTATION_FAILURE,
                    worldFailure.getMessage());
        }
    }
}
