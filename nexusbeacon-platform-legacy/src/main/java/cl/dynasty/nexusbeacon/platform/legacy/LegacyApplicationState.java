package cl.dynasty.nexusbeacon.platform.legacy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/** Authoritative in-process registry with persist-before-publish mutations. */
public final class LegacyApplicationState {
    private final LegacyBeaconStorage storage;
    private final LegacyStatePublication publication;
    private Map<LegacyBeaconLocation, LegacyBeaconState> byLocation =
            Collections.emptyMap();
    private Map<UUID, LegacyBeaconState> byUniqueId = Collections.emptyMap();
    private LegacyApplicationStateStatus status = LegacyApplicationStateStatus.NEW;

    public LegacyApplicationState(LegacyBeaconStorage storage) {
        this(storage, LegacyStatePublication.DIRECT);
    }

    LegacyApplicationState(LegacyBeaconStorage storage, LegacyStatePublication publication) {
        if (storage == null) throw new NullPointerException("storage");
        if (publication == null) throw new NullPointerException("publication");
        this.storage = storage;
        this.publication = publication;
    }

    public synchronized LegacyStorageLoadResult initialize() {
        if (status != LegacyApplicationStateStatus.NEW) {
            throw new IllegalStateException("Legacy application state has already been initialized");
        }
        LegacyStorageLoadResult result = storage.load();
        if (!result.isSuccessful()) {
            status = LegacyApplicationStateStatus.FAILED;
            return result;
        }
        try {
            publish(index(result.getBeacons()));
            status = byLocation.isEmpty() ? LegacyApplicationStateStatus.READY_EMPTY
                    : LegacyApplicationStateStatus.READY;
            return result;
        } catch (IllegalArgumentException exception) {
            status = LegacyApplicationStateStatus.FAILED;
            return LegacyStorageLoadResult.failure(LegacyStorageLoadStatus.CORRUPT,
                    "Loaded state violates registry invariants: " + exception.getMessage());
        }
    }

    public synchronized boolean insert(LegacyBeaconState beacon) {
        requireReady();
        if (beacon == null) throw new NullPointerException("beacon");
        if (byLocation.containsKey(beacon.getLocation()) || byUniqueId.containsKey(beacon.getUniqueId())) {
            return false;
        }
        TreeMap<LegacyBeaconLocation, LegacyBeaconState> prospective =
                new TreeMap<LegacyBeaconLocation, LegacyBeaconState>(byLocation);
        prospective.put(beacon.getLocation(), beacon);
        commit(prospective);
        return true;
    }

    public synchronized boolean update(LegacyBeaconState beacon) {
        requireReady();
        if (beacon == null) throw new NullPointerException("beacon");
        LegacyBeaconState current = byLocation.get(beacon.getLocation());
        if (current == null || !current.getUniqueId().equals(beacon.getUniqueId())) return false;
        TreeMap<LegacyBeaconLocation, LegacyBeaconState> prospective =
                new TreeMap<LegacyBeaconLocation, LegacyBeaconState>(byLocation);
        prospective.put(beacon.getLocation(), beacon);
        commit(prospective);
        return true;
    }

    public synchronized boolean delete(LegacyBeaconLocation location) {
        requireReady();
        if (location == null) throw new NullPointerException("location");
        if (!byLocation.containsKey(location)) return false;
        TreeMap<LegacyBeaconLocation, LegacyBeaconState> prospective =
                new TreeMap<LegacyBeaconLocation, LegacyBeaconState>(byLocation);
        prospective.remove(location);
        commit(prospective);
        return true;
    }

    public synchronized LegacyBeaconState find(LegacyBeaconLocation location) {
        requireReady();
        return byLocation.get(location);
    }

    public synchronized LegacyBeaconState findByUniqueId(UUID uniqueId) {
        requireReady();
        return byUniqueId.get(uniqueId);
    }

    public synchronized List<LegacyBeaconState> findByOwner(UUID owner) {
        requireReady();
        List<LegacyBeaconState> matches = new ArrayList<LegacyBeaconState>();
        for (LegacyBeaconState beacon : byLocation.values()) {
            if (owner == null ? beacon.getOwner() == null : owner.equals(beacon.getOwner())) matches.add(beacon);
        }
        return Collections.unmodifiableList(matches);
    }

    public synchronized Collection<LegacyBeaconState> snapshot() {
        requireReady();
        return Collections.unmodifiableList(new ArrayList<LegacyBeaconState>(byLocation.values()));
    }

    public synchronized int size() {
        requireReady();
        return byLocation.size();
    }

    public synchronized LegacyApplicationStateStatus getStatus() { return status; }
    public String getBackendName() { return storage.getBackendName(); }

    public synchronized void close() {
        if (status == LegacyApplicationStateStatus.CLOSED) return;
        storage.close();
        byLocation = Collections.emptyMap();
        byUniqueId = Collections.emptyMap();
        status = LegacyApplicationStateStatus.CLOSED;
    }

    private void commit(Map<LegacyBeaconLocation, LegacyBeaconState> prospective) {
        Index previous = new Index(new TreeMap<LegacyBeaconLocation, LegacyBeaconState>(byLocation),
                new LinkedHashMap<UUID, LegacyBeaconState>(byUniqueId));
        Index indexed = index(prospective.values());
        storage.store(indexed.byLocation.values());
        try {
            publication.beforePublish();
            publish(indexed);
        } catch (RuntimeException publicationFailure) {
            try {
                storage.store(previous.byLocation.values());
                publish(previous);
            } catch (RuntimeException recoveryFailure) {
                publicationFailure.addSuppressed(recoveryFailure);
                status = LegacyApplicationStateStatus.FAILED;
                throw new LegacyStorageException("Runtime publication and durable recovery both failed",
                        publicationFailure);
            }
            throw new LegacyStorageException("Runtime publication failed; durable state was restored",
                    publicationFailure);
        }
        status = byLocation.isEmpty() ? LegacyApplicationStateStatus.READY_EMPTY
                : LegacyApplicationStateStatus.READY;
    }

    private void publish(Index index) {
        byLocation = Collections.unmodifiableMap(index.byLocation);
        byUniqueId = Collections.unmodifiableMap(index.byUniqueId);
    }

    private static Index index(Collection<LegacyBeaconState> beacons) {
        TreeMap<LegacyBeaconLocation, LegacyBeaconState> locations =
                new TreeMap<LegacyBeaconLocation, LegacyBeaconState>();
        LinkedHashMap<UUID, LegacyBeaconState> uniqueIds = new LinkedHashMap<UUID, LegacyBeaconState>();
        for (LegacyBeaconState beacon : beacons) {
            if (beacon == null || locations.put(beacon.getLocation(), beacon) != null
                    || uniqueIds.put(beacon.getUniqueId(), beacon) != null) {
                throw new IllegalArgumentException("Duplicate or null beacon state");
            }
        }
        return new Index(locations, uniqueIds);
    }

    private void requireReady() {
        if (!status.isReady()) throw new IllegalStateException("Legacy application state is " + status);
    }

    private static final class Index {
        private final TreeMap<LegacyBeaconLocation, LegacyBeaconState> byLocation;
        private final LinkedHashMap<UUID, LegacyBeaconState> byUniqueId;

        private Index(TreeMap<LegacyBeaconLocation, LegacyBeaconState> byLocation,
                LinkedHashMap<UUID, LegacyBeaconState> byUniqueId) {
            this.byLocation = byLocation;
            this.byUniqueId = byUniqueId;
        }
    }
}
