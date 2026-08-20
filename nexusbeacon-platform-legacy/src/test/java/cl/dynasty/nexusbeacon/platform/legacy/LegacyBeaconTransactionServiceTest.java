package cl.dynasty.nexusbeacon.platform.legacy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

class LegacyBeaconTransactionServiceTest {
    @Test void markedItemIsAcceptedAndPublishedOnlyAfterPersistence() {
        Fixture fixture = new Fixture();
        ItemStack marked = fixture.item(LegacyIdentityStatus.RECOGNIZED);
        fixture.storage.observe = () -> assertEquals(0, fixture.state.size());
        LegacyBeaconTransactionResult result = fixture.transactions.place(marked, fixture.location, fixture.owner);
        assertTrue(result.isCommitted());
        assertSame(result.getBeacon(), fixture.state.find(fixture.location));
        assertEquals(1, fixture.storage.lastStored.size());
    }

    @Test void vanillaBeaconIsRejectedWithoutWrite() {
        assertRejected(LegacyIdentityStatus.NOT_RECOGNIZED,
                LegacyBeaconTransactionStatus.NOT_RECOGNIZED);
    }

    @Test void displayNameSpoofIsRejectedWithoutWrite() {
        assertRejected(LegacyIdentityStatus.NOT_RECOGNIZED,
                LegacyBeaconTransactionStatus.NOT_RECOGNIZED);
    }

    @Test void loreSpoofIsRejectedWithoutWrite() {
        assertRejected(LegacyIdentityStatus.NOT_RECOGNIZED,
                LegacyBeaconTransactionStatus.NOT_RECOGNIZED);
    }

    @Test void malformedOrWrongMarkerIsRejectedWithoutWrite() {
        assertRejected(LegacyIdentityStatus.MALFORMED,
                LegacyBeaconTransactionStatus.MALFORMED_ITEM);
        assertRejected(LegacyIdentityStatus.UNSUPPORTED,
                LegacyBeaconTransactionStatus.MALFORMED_ITEM);
    }

    @Test void portableIdentityDataIsPreservedIntoPlacedState() {
        Fixture fixture = new Fixture();
        ItemStack marked = fixture.item(LegacyIdentityStatus.RECOGNIZED);
        UUID uniqueId = UUID.fromString("33333333-3333-4333-8333-333333333333");
        fixture.bridge.data.put(marked, new LegacyPortableBeaconData(uniqueId,
                Collections.singletonMap("speed", Integer.valueOf(2)), Collections.singleton("speed")));
        LegacyBeaconState beacon = fixture.transactions.place(marked, fixture.location, fixture.owner).getBeacon();
        assertEquals(uniqueId, beacon.getUniqueId());
        assertEquals(Integer.valueOf(2), beacon.getEffectLevels().get("speed"));
        assertTrue(beacon.getActiveEffects().contains("speed"));
    }

    @Test void duplicateLocationAndUniqueIdAreRejectedBeforeASecondWrite() {
        Fixture fixture = new Fixture();
        ItemStack first = fixture.item(LegacyIdentityStatus.RECOGNIZED);
        LegacyBeaconState placed = fixture.transactions.place(first, fixture.location, fixture.owner).getBeacon();
        int writes = fixture.storage.writes;
        assertEquals(LegacyBeaconTransactionStatus.DUPLICATE_LOCATION_OR_ID,
                fixture.transactions.place(fixture.item(LegacyIdentityStatus.RECOGNIZED),
                        fixture.location, fixture.owner).getStatus());
        ItemStack sameId = fixture.item(LegacyIdentityStatus.RECOGNIZED);
        fixture.bridge.data.put(sameId, new LegacyPortableBeaconData(placed.getUniqueId(),
                Collections.<String, Integer>emptyMap(), Collections.<String>emptySet()));
        assertEquals(LegacyBeaconTransactionStatus.DUPLICATE_LOCATION_OR_ID,
                fixture.transactions.place(sameId, new LegacyBeaconLocation("world", 2, 64, 2),
                        fixture.owner).getStatus());
        assertEquals(writes, fixture.storage.writes);
    }

    @Test void dottedWorldIsRejectedBeforeAnyMutation() {
        Fixture fixture = new Fixture();
        assertThrows(IllegalArgumentException.class,
                () -> new LegacyBeaconLocation("world.test", 1, 64, 2));
        assertEquals(0, fixture.storage.writes);
        assertEquals(0, fixture.state.size());
    }

    @Test void saveFailureLeavesDiskAndRuntimeEmpty() {
        Fixture fixture = new Fixture();
        fixture.storage.failWrites = true;
        LegacyBeaconTransactionResult result = fixture.transactions.place(
                fixture.item(LegacyIdentityStatus.RECOGNIZED), fixture.location, fixture.owner);
        assertEquals(LegacyBeaconTransactionStatus.STORAGE_FAILURE, result.getStatus());
        assertNull(fixture.state.find(fixture.location));
        assertTrue(fixture.storage.lastStored.isEmpty());
    }

    @Test void unavailableStateFailsClosed() {
        RecordingStorage storage = new RecordingStorage();
        storage.loadResult = LegacyStorageLoadResult.failure(LegacyStorageLoadStatus.UNAVAILABLE, "offline");
        LegacyApplicationState state = new LegacyApplicationState(storage);
        state.initialize();
        FakeBridge bridge = new FakeBridge();
        ItemStack item = new ItemStack(Material.BEACON);
        bridge.status.put(item, LegacyIdentityStatus.RECOGNIZED);
        LegacyBeaconTransactionResult result = new LegacyBeaconTransactionService(state,
                new LegacyItemIdentityService(bridge), settings()).place(item,
                        new LegacyBeaconLocation("world", 1, 64, 2), UUID.randomUUID());
        assertEquals(LegacyBeaconTransactionStatus.STORAGE_FAILURE, result.getStatus());
        assertEquals(0, storage.writes);
    }

    @Test void removalPersistsBeforeWorldMutation() {
        Fixture fixture = placedFixture();
        RecordingWorld world = new RecordingWorld();
        fixture.storage.observe = () -> assertFalse(world.removed);
        LegacyBeaconTransactionResult result = fixture.transactions.remove(fixture.location, world);
        assertTrue(result.isCommitted());
        assertTrue(world.removed);
        assertNull(fixture.state.find(fixture.location));
    }

    @Test void deleteFailureLeavesStateAndWorldAuthoritative() {
        Fixture fixture = placedFixture();
        LegacyBeaconState before = fixture.state.find(fixture.location);
        fixture.storage.failWrites = true;
        RecordingWorld world = new RecordingWorld();
        LegacyBeaconTransactionResult result = fixture.transactions.remove(fixture.location, world);
        assertEquals(LegacyBeaconTransactionStatus.STORAGE_FAILURE, result.getStatus());
        assertSame(before, fixture.state.find(fixture.location));
        assertFalse(world.removed);
    }

    @Test void worldRemovalFailureCompensatesDurableAndRuntimeState() {
        Fixture fixture = placedFixture();
        LegacyBeaconState before = fixture.state.find(fixture.location);
        RecordingWorld world = new RecordingWorld();
        world.failRemoval = true;
        LegacyBeaconTransactionResult result = fixture.transactions.remove(fixture.location, world);
        assertEquals(LegacyBeaconTransactionStatus.WORLD_MUTATION_FAILURE, result.getStatus());
        assertSame(before, fixture.state.find(fixture.location));
        assertEquals(Collections.singletonList(before), fixture.storage.lastStored);
    }

    @Test void physicalBeaconWithoutStateIsUnmanagedAndMismatchDoesNotDeleteState() {
        Fixture empty = new Fixture();
        RecordingWorld physicalBeacon = new RecordingWorld();
        assertEquals(LegacyBeaconTransactionStatus.UNMANAGED_BEACON,
                empty.transactions.remove(empty.location, physicalBeacon).getStatus());
        assertFalse(physicalBeacon.removed);

        Fixture placed = placedFixture();
        RecordingWorld missingBlock = new RecordingWorld();
        missingBlock.beacon = false;
        assertEquals(LegacyBeaconTransactionStatus.STATE_WORLD_MISMATCH,
                placed.transactions.remove(placed.location, missingBlock).getStatus());
        assertEquals(1, placed.state.size());
    }

    private static void assertRejected(LegacyIdentityStatus identity,
            LegacyBeaconTransactionStatus expected) {
        Fixture fixture = new Fixture();
        LegacyBeaconTransactionResult result = fixture.transactions.place(fixture.item(identity),
                fixture.location, fixture.owner);
        assertEquals(expected, result.getStatus());
        assertEquals(0, fixture.storage.writes);
        assertEquals(0, fixture.state.size());
    }

    private static Fixture placedFixture() {
        Fixture fixture = new Fixture();
        assertTrue(fixture.transactions.place(fixture.item(LegacyIdentityStatus.RECOGNIZED),
                fixture.location, fixture.owner).isCommitted());
        return fixture;
    }

    private static LegacyBeaconGameplaySettings settings() {
        return new LegacyBeaconGameplaySettings(48, true, true, "VILLAGER_HAPPY",
                true, true, true, true);
    }

    private static final class Fixture {
        private final RecordingStorage storage = new RecordingStorage();
        private final LegacyApplicationState state = new LegacyApplicationState(storage);
        private final FakeBridge bridge = new FakeBridge();
        private final LegacyBeaconTransactionService transactions;
        private final LegacyBeaconLocation location = new LegacyBeaconLocation("world", 1, 64, 2);
        private final UUID owner = UUID.fromString("22222222-2222-4222-8222-222222222222");

        private Fixture() {
            state.initialize();
            transactions = new LegacyBeaconTransactionService(state,
                    new LegacyItemIdentityService(bridge), settings());
        }

        private ItemStack item(LegacyIdentityStatus identity) {
            ItemStack item = new ItemStack(Material.BEACON);
            bridge.status.put(item, identity);
            return item;
        }
    }

    private static final class FakeBridge implements LegacyNbtBridge {
        private final Map<ItemStack, LegacyIdentityStatus> status =
                new IdentityHashMap<ItemStack, LegacyIdentityStatus>();
        private final Map<ItemStack, LegacyPortableBeaconData> data =
                new IdentityHashMap<ItemStack, LegacyPortableBeaconData>();
        @Override public ItemStack mark(ItemStack item) { status.put(item, LegacyIdentityStatus.RECOGNIZED); return item; }
        @Override public LegacyIdentityStatus identify(ItemStack item) {
            LegacyIdentityStatus value = status.get(item);
            return value == null ? LegacyIdentityStatus.NOT_RECOGNIZED : value;
        }
        @Override public ItemStack writePortableData(ItemStack item, LegacyPortableBeaconData portable) {
            mark(item); data.put(item, portable); return item;
        }
        @Override public Optional<LegacyPortableBeaconData> readPortableData(ItemStack item) {
            return Optional.ofNullable(data.get(item));
        }
        @Override public String getRevision() { return "test"; }
    }

    private static final class RecordingStorage implements LegacyBeaconStorage {
        private LegacyStorageLoadResult loadResult =
                LegacyStorageLoadResult.success(Collections.<LegacyBeaconState>emptyList());
        private List<LegacyBeaconState> lastStored = Collections.emptyList();
        private boolean failWrites;
        private int writes;
        private Runnable observe;
        @Override public LegacyStorageLoadResult load() { return loadResult; }
        @Override public void store(Collection<LegacyBeaconState> beacons) {
            writes++;
            if (observe != null) { Runnable current = observe; observe = null; current.run(); }
            if (failWrites) throw new LegacyStorageException("synthetic failure");
            lastStored = Collections.unmodifiableList(new ArrayList<LegacyBeaconState>(beacons));
        }
        @Override public void close() { }
        @Override public String getBackendName() { return "TEST"; }
    }

    private static final class RecordingWorld implements LegacyWorldBeaconMutation {
        private boolean beacon = true;
        private boolean removed;
        private boolean failRemoval;
        @Override public boolean isBeacon() { return beacon; }
        @Override public void removeBeacon() {
            if (failRemoval) throw new IllegalStateException("synthetic world failure");
            removed = true;
            beacon = false;
        }
    }
}
