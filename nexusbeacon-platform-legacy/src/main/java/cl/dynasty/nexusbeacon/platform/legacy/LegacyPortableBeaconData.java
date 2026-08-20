package cl.dynasty.nexusbeacon.platform.legacy;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;

/** The state which travels with a broken NexusBeacon item. */
public final class LegacyPortableBeaconData {
    private final UUID uniqueId;
    private final Map<String, Integer> effectLevels;
    private final Set<String> activeEffects;

    public LegacyPortableBeaconData(UUID uniqueId, Map<String, Integer> effectLevels,
            Set<String> activeEffects) {
        if (uniqueId == null) throw new IllegalArgumentException("uniqueId is required");
        TreeMap<String, Integer> effects = new TreeMap<String, Integer>();
        if (effectLevels != null) effects.putAll(effectLevels);
        TreeSet<String> active = new TreeSet<String>();
        if (activeEffects != null) active.addAll(activeEffects);
        for (Map.Entry<String, Integer> entry : effects.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isEmpty()
                    || entry.getValue() == null || entry.getValue().intValue() <= 0) {
                throw new IllegalArgumentException("Invalid portable effect");
            }
        }
        if (!effects.keySet().containsAll(active)) {
            throw new IllegalArgumentException("Active portable effect was not acquired");
        }
        this.uniqueId = uniqueId;
        this.effectLevels = Collections.unmodifiableMap(effects);
        this.activeEffects = Collections.unmodifiableSet(active);
    }

    public UUID getUniqueId() { return uniqueId; }
    public Map<String, Integer> getEffectLevels() { return effectLevels; }
    public Set<String> getActiveEffects() { return activeEffects; }
}
