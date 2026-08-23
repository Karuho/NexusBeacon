package cl.dynasty.nexusbeacon.platform.legacy;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;

/** Immutable persistent beacon state. Runtime power and resolved worlds remain derived. */
public final class LegacyBeaconState {
    private final LegacyBeaconLocation location;
    private final UUID uniqueId;
    private final UUID owner;
    private final int range;
    private final int level;
    private final Map<String, Integer> effectLevels;
    private final Set<String> activeEffects;
    private final Set<UUID> trustedPlayers;
    private final boolean protectBaseBlocks;
    private final String beamStyle;
    private final boolean rangeParticlesEnabled;
    private final String rangeParticleType;

    public LegacyBeaconState(LegacyBeaconLocation location, UUID uniqueId, UUID owner, int range, int level,
            Map<String, Integer> effectLevels, Set<String> activeEffects, Set<UUID> trustedPlayers,
            boolean protectBaseBlocks, String beamStyle, boolean rangeParticlesEnabled, String rangeParticleType) {
        if (location == null) throw new IllegalArgumentException("Location is required");
        if (uniqueId == null) throw new IllegalArgumentException("Unique id is required");
        if (range <= 0 || level <= 0) throw new IllegalArgumentException("Range and level must be positive");
        this.location = location;
        this.uniqueId = uniqueId;
        this.owner = owner;
        this.range = range;
        this.level = level;
        this.effectLevels = immutableEffects(effectLevels);
        this.activeEffects = immutableActiveEffects(activeEffects, this.effectLevels);
        this.trustedPlayers = immutableTrustedPlayers(trustedPlayers);
        this.protectBaseBlocks = protectBaseBlocks;
        this.beamStyle = optionalIdentifier(beamStyle, "beam style");
        this.rangeParticlesEnabled = rangeParticlesEnabled;
        this.rangeParticleType = requiredIdentifier(rangeParticleType, "range particle type");
    }

    public String getId() { return location.toStorageKey(); }
    public LegacyBeaconLocation getLocation() { return location; }
    public UUID getUniqueId() { return uniqueId; }
    public UUID getOwner() { return owner; }
    public int getRange() { return range; }
    public int getLevel() { return level; }
    public Map<String, Integer> getEffectLevels() { return effectLevels; }
    public Set<String> getActiveEffects() { return activeEffects; }
    public Set<UUID> getTrustedPlayers() { return trustedPlayers; }
    public boolean isProtectBaseBlocks() { return protectBaseBlocks; }
    public String getBeamStyle() { return beamStyle; }
    public boolean isRangeParticlesEnabled() { return rangeParticlesEnabled; }
    public String getRangeParticleType() { return rangeParticleType; }

    public LegacyBeaconState withTrustedPlayers(Set<UUID> trusted) {
        return new LegacyBeaconState(location, uniqueId, owner, range, level, effectLevels,
                activeEffects, trusted, protectBaseBlocks, beamStyle, rangeParticlesEnabled,
                rangeParticleType);
    }

    public LegacyBeaconState withActiveEffect(String effectId, boolean active) {
        String normalized = requiredIdentifier(effectId, "effect id").toLowerCase(java.util.Locale.ROOT);
        if (!effectLevels.containsKey(normalized)) {
            throw new IllegalArgumentException("Effect was not acquired: " + normalized);
        }
        TreeSet<String> replacement = new TreeSet<String>(activeEffects);
        if (active) replacement.add(normalized); else replacement.remove(normalized);
        return new LegacyBeaconState(location, uniqueId, owner, range, level, effectLevels,
                replacement, trustedPlayers, protectBaseBlocks, beamStyle, rangeParticlesEnabled,
                rangeParticleType);
    }

    public LegacyBeaconState withBeamStyle(String style) {
        return new LegacyBeaconState(location, uniqueId, owner, range, level, effectLevels,
                activeEffects, trustedPlayers, protectBaseBlocks, style, rangeParticlesEnabled,
                rangeParticleType);
    }

    public LegacyBeaconState withRangeParticles(boolean enabled, String particleType) {
        return new LegacyBeaconState(location, uniqueId, owner, range, level, effectLevels,
                activeEffects, trustedPlayers, protectBaseBlocks, beamStyle, enabled, particleType);
    }

    public LegacyBeaconState withEffectLevel(String effectId, int effectLevel, boolean activate) {
        String normalized = requiredIdentifier(effectId, "effect id").toLowerCase(java.util.Locale.ROOT);
        if (effectLevel <= 0) throw new IllegalArgumentException("Effect level must be positive");
        TreeMap<String, Integer> levels = new TreeMap<String, Integer>(effectLevels);
        levels.put(normalized, Integer.valueOf(effectLevel));
        TreeSet<String> active = new TreeSet<String>(activeEffects);
        if (activate) active.add(normalized);
        return new LegacyBeaconState(location, uniqueId, owner, range, level, levels,
                active, trustedPlayers, protectBaseBlocks, beamStyle, rangeParticlesEnabled,
                rangeParticleType);
    }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof LegacyBeaconState)) return false;
        LegacyBeaconState state = (LegacyBeaconState) other;
        return range == state.range && level == state.level
                && protectBaseBlocks == state.protectBaseBlocks
                && rangeParticlesEnabled == state.rangeParticlesEnabled
                && location.equals(state.location) && uniqueId.equals(state.uniqueId)
                && equal(owner, state.owner) && effectLevels.equals(state.effectLevels)
                && activeEffects.equals(state.activeEffects) && trustedPlayers.equals(state.trustedPlayers)
                && equal(beamStyle, state.beamStyle) && rangeParticleType.equals(state.rangeParticleType);
    }

    @Override public int hashCode() {
        int result = location.hashCode();
        result = 31 * result + uniqueId.hashCode();
        result = 31 * result + (owner == null ? 0 : owner.hashCode());
        result = 31 * result + range;
        result = 31 * result + level;
        result = 31 * result + effectLevels.hashCode();
        result = 31 * result + activeEffects.hashCode();
        result = 31 * result + trustedPlayers.hashCode();
        result = 31 * result + (protectBaseBlocks ? 1 : 0);
        result = 31 * result + (beamStyle == null ? 0 : beamStyle.hashCode());
        result = 31 * result + (rangeParticlesEnabled ? 1 : 0);
        return 31 * result + rangeParticleType.hashCode();
    }

    private static Map<String, Integer> immutableEffects(Map<String, Integer> effects) {
        TreeMap<String, Integer> copy = new TreeMap<String, Integer>();
        if (effects != null) {
            for (Map.Entry<String, Integer> entry : effects.entrySet()) {
                String id = requiredIdentifier(entry.getKey(), "effect id").toLowerCase(java.util.Locale.ROOT);
                Integer value = entry.getValue();
                if (value == null || value.intValue() <= 0 || copy.put(id, value) != null) {
                    throw new IllegalArgumentException("Effect levels must be positive and unique");
                }
            }
        }
        return Collections.unmodifiableMap(copy);
    }

    private static Set<String> immutableActiveEffects(Set<String> active, Map<String, Integer> effects) {
        TreeSet<String> copy = new TreeSet<String>();
        if (active != null) {
            for (String raw : active) {
                String id = requiredIdentifier(raw, "active effect id").toLowerCase(java.util.Locale.ROOT);
                if (!effects.containsKey(id)) throw new IllegalArgumentException("Active effect was not acquired: " + id);
                copy.add(id);
            }
        }
        return Collections.unmodifiableSet(copy);
    }

    private static Set<UUID> immutableTrustedPlayers(Set<UUID> trusted) {
        TreeSet<UUID> copy = new TreeSet<UUID>();
        if (trusted != null) {
            for (UUID uuid : trusted) {
                if (uuid == null) throw new IllegalArgumentException("Trusted player id cannot be null");
                copy.add(uuid);
            }
        }
        return Collections.unmodifiableSet(copy);
    }

    private static String optionalIdentifier(String value, String name) {
        if (value == null) return null;
        return requiredIdentifier(value, name);
    }

    private static String requiredIdentifier(String value, String name) {
        if (value == null || value.isEmpty() || !value.equals(value.trim()) || value.length() > 128) {
            throw new IllegalArgumentException("Invalid " + name);
        }
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (!(Character.isLetterOrDigit(character) || character == '_' || character == '-'
                    || character == '.' || character == ':')) {
                throw new IllegalArgumentException("Invalid " + name);
            }
        }
        return value;
    }

    private static boolean equal(Object first, Object second) {
        return first == null ? second == null : first.equals(second);
    }
}
