package cl.dynasty.nexusbeacon.platform.legacy;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Owns transient GUI state without retaining Player objects. */
public final class LegacyGuiSessionRegistry {
    private final Map<UUID, LegacyGuiSession> sessions = new HashMap<UUID, LegacyGuiSession>();
    private long generation;

    public synchronized LegacyGuiSession replace(UUID playerId, LegacyBeaconState beacon, LegacyGuiMenu menu) {
        LegacyGuiSession session = new LegacyGuiSession(playerId, beacon.getUniqueId(), beacon.getLocation(),
                menu, ++generation);
        sessions.put(playerId, session);
        return session;
    }

    public synchronized LegacyGuiSession get(UUID playerId) { return sessions.get(playerId); }

    public synchronized boolean isCurrent(LegacyGuiSession session) {
        return session != null && session == sessions.get(session.getPlayerId());
    }

    public synchronized boolean removeIfCurrent(LegacyGuiSession session) {
        if (!isCurrent(session)) return false;
        sessions.remove(session.getPlayerId());
        return true;
    }

    public synchronized void remove(UUID playerId) { sessions.remove(playerId); }
    public synchronized void clear() { sessions.clear(); }
    public synchronized int size() { return sessions.size(); }
}
