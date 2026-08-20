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
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class LegacyApplicationStateTest {
    @Test void initializesEmptyExactlyOnceAndExposesReadiness() {
        RecordingStorage storage = new RecordingStorage();
        LegacyApplicationState state = new LegacyApplicationState(storage);
        assertEquals(LegacyStorageLoadStatus.EMPTY, state.initialize().getStatus());
        assertEquals(LegacyApplicationStateStatus.READY_EMPTY, state.getStatus());
        assertEquals(0, state.size());
        assertThrows(IllegalStateException.class, state::initialize);
    }

    @Test void insertsAndIndexesByLocationUniqueIdAndOwner() {
        RecordingStorage storage = new RecordingStorage();
        LegacyApplicationState state = ready(storage);
        LegacyBeaconState beacon = beacon("world", 1, "11111111-1111-4111-8111-111111111111", 48);
        assertTrue(state.insert(beacon));
        assertSame(beacon, state.find(beacon.getLocation()));
        assertSame(beacon, state.findByUniqueId(beacon.getUniqueId()));
        assertEquals(Collections.singletonList(beacon), state.findByOwner(beacon.getOwner()));
        assertEquals(LegacyApplicationStateStatus.READY, state.getStatus());
        assertEquals(Collections.singletonList(beacon), storage.lastStored);
    }

    @Test void rejectsDuplicateLocationsAndUniqueIdsWithoutWriting() {
        RecordingStorage storage = new RecordingStorage();
        LegacyApplicationState state = ready(storage);
        LegacyBeaconState first = beacon("world", 1, "11111111-1111-4111-8111-111111111111", 48);
        assertTrue(state.insert(first));
        int writes = storage.writes;
        assertFalse(state.insert(beacon("world", 1, "44444444-4444-4444-8444-444444444444", 20)));
        assertFalse(state.insert(beacon("other", 3, first.getUniqueId().toString(), 20)));
        assertEquals(writes, storage.writes);
        assertEquals(1, state.size());
    }

    @Test void failedInsertDoesNotPublishProspectiveState() {
        RecordingStorage storage = new RecordingStorage();
        LegacyApplicationState state = ready(storage);
        storage.failWrites = true;
        LegacyBeaconState beacon = beacon("world", 1, "11111111-1111-4111-8111-111111111111", 48);
        assertThrows(LegacyStorageException.class, () -> state.insert(beacon));
        assertEquals(0, state.size());
        assertNull(state.find(beacon.getLocation()));
    }

    @Test void updateAndDeleteCommitCompleteProspectiveSnapshots() {
        RecordingStorage storage = new RecordingStorage();
        LegacyApplicationState state = ready(storage);
        LegacyBeaconState initial = beacon("world", 1, "11111111-1111-4111-8111-111111111111", 48);
        state.insert(initial);
        LegacyBeaconState updated = beacon("world", 1, initial.getUniqueId().toString(), 72);
        assertTrue(state.update(updated));
        assertEquals(72, state.find(initial.getLocation()).getRange());
        assertTrue(state.delete(initial.getLocation()));
        assertTrue(storage.lastStored.isEmpty());
        assertEquals(LegacyApplicationStateStatus.READY_EMPTY, state.getStatus());
    }

    @Test void failedDeleteRetainsTheAuthoritativeRecord() {
        RecordingStorage storage = new RecordingStorage();
        LegacyApplicationState state = ready(storage);
        LegacyBeaconState beacon = beacon("world", 1, "11111111-1111-4111-8111-111111111111", 48);
        state.insert(beacon);
        storage.failWrites = true;
        assertThrows(LegacyStorageException.class, () -> state.delete(beacon.getLocation()));
        assertSame(beacon, state.find(beacon.getLocation()));
    }

    @Test void failedLoadNeverBecomesAnEmptyReadyRegistry() {
        RecordingStorage storage = new RecordingStorage();
        storage.loadResult = LegacyStorageLoadResult.failure(LegacyStorageLoadStatus.CORRUPT, "invalid");
        LegacyApplicationState state = new LegacyApplicationState(storage);
        assertEquals(LegacyStorageLoadStatus.CORRUPT, state.initialize().getStatus());
        assertEquals(LegacyApplicationStateStatus.FAILED, state.getStatus());
        assertThrows(IllegalStateException.class, state::size);
    }

    @Test void runtimePublishFailureRestoresPreviousDurableAndRuntimeSnapshot() {
        RecordingStorage storage = new RecordingStorage();
        LegacyApplicationState state = new LegacyApplicationState(storage, new LegacyStatePublication() {
            @Override public void beforePublish() { throw new IllegalStateException("synthetic publish failure"); }
        });
        state.initialize();
        LegacyBeaconState beacon = beacon("world", 1, "11111111-1111-4111-8111-111111111111", 48);
        assertThrows(LegacyStorageException.class, () -> state.insert(beacon));
        assertNull(state.find(beacon.getLocation()));
        assertTrue(storage.lastStored.isEmpty());
        assertEquals(2, storage.writes);
        assertEquals(LegacyApplicationStateStatus.READY_EMPTY, state.getStatus());
    }

    @Test void failedRuntimeRecoveryMarksStateFailed() {
        RecordingStorage storage = new RecordingStorage();
        LegacyApplicationState state = new LegacyApplicationState(storage, new LegacyStatePublication() {
            @Override public void beforePublish() { storage.failWrites = true; throw new IllegalStateException("fail"); }
        });
        state.initialize();
        assertThrows(LegacyStorageException.class, () -> state.insert(
                beacon("world", 1, "11111111-1111-4111-8111-111111111111", 48)));
        assertEquals(LegacyApplicationStateStatus.FAILED, state.getStatus());
    }

    private static LegacyApplicationState ready(RecordingStorage storage) {
        LegacyApplicationState state = new LegacyApplicationState(storage);
        state.initialize();
        return state;
    }

    private static LegacyBeaconState beacon(String world, int x, String uniqueId, int range) {
        return new LegacyBeaconState(new LegacyBeaconLocation(world, x, 64, 2), UUID.fromString(uniqueId),
                UUID.fromString("22222222-2222-4222-8222-222222222222"), range, 1,
                Collections.<String, Integer>emptyMap(), Collections.<String>emptySet(),
                Collections.<UUID>emptySet(), true, null, true, "VILLAGER_HAPPY");
    }

    private static final class RecordingStorage implements LegacyBeaconStorage {
        private LegacyStorageLoadResult loadResult =
                LegacyStorageLoadResult.success(Collections.<LegacyBeaconState>emptyList());
        private List<LegacyBeaconState> lastStored = Collections.emptyList();
        private boolean failWrites;
        private int writes;

        @Override public LegacyStorageLoadResult load() { return loadResult; }

        @Override public void store(Collection<LegacyBeaconState> beacons) {
            writes++;
            if (failWrites) throw new LegacyStorageException("synthetic write failure");
            lastStored = Collections.unmodifiableList(new ArrayList<LegacyBeaconState>(beacons));
        }

        @Override public void close() { }
        @Override public String getBackendName() { return "TEST"; }
    }
}
