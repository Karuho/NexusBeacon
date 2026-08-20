package cl.dynasty.nexusbeacon.platform.legacy;

import java.util.UUID;

/** Immutable identity for one open Legacy GUI generation. */
public final class LegacyGuiSession {
    private final UUID playerId;
    private final UUID beaconId;
    private final LegacyBeaconLocation location;
    private final LegacyGuiMenu menu;
    private final long generation;

    LegacyGuiSession(UUID playerId, UUID beaconId, LegacyBeaconLocation location,
            LegacyGuiMenu menu, long generation) {
        if (playerId == null || beaconId == null || location == null || menu == null) {
            throw new NullPointerException();
        }
        this.playerId = playerId;
        this.beaconId = beaconId;
        this.location = location;
        this.menu = menu;
        this.generation = generation;
    }

    public UUID getPlayerId() { return playerId; }
    public UUID getBeaconId() { return beaconId; }
    public LegacyBeaconLocation getLocation() { return location; }
    public LegacyGuiMenu getMenu() { return menu; }
    public long getGeneration() { return generation; }
}
