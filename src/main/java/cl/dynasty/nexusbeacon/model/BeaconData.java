package cl.dynasty.nexusbeacon.model;

import org.bukkit.Location;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class BeaconData {

    private final String id;
    private final String uniqueId;
    private final Location location;
    private final UUID owner;
    private int range;
    private int level;
    private final Map<String, Integer> effectLevels;
    private final Set<String> activeEffects;
    private final Set<UUID> trustedPlayers;
    private boolean protectBaseBlocks;
    private String beamStyle;
    private boolean rangeParticlesEnabled = true;
    private String rangeParticleType = "VILLAGER_HAPPY";

    public BeaconData(String id, String uniqueId, Location location, UUID owner, int range, int level,
            Map<String, Integer> effectLevels, Set<String> activeEffects,
            Set<UUID> trustedPlayers, boolean protectBaseBlocks, String beamStyle) {
        this.id = id;
        this.uniqueId = uniqueId;
        this.location = location;
        this.owner = owner;
        this.range = range;
        this.level = level;
        this.effectLevels = effectLevels != null ? effectLevels : new HashMap<String, Integer>();
        this.activeEffects = activeEffects != null ? activeEffects : new HashSet<String>();
        this.trustedPlayers = trustedPlayers != null ? trustedPlayers : new HashSet<UUID>();
        this.protectBaseBlocks = protectBaseBlocks;
        this.beamStyle = beamStyle;
    }

    public String getId() {
        return id;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public Location getLocation() {
        return location;
    }

    public UUID getOwner() {
        return owner;
    }

    public int getRange() {
        return range;
    }

    public void setRange(int range) {
        this.range = range;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public Map<String, Integer> getEffectLevels() {
        return effectLevels;
    }

    public Set<String> getActiveEffects() {
        return activeEffects;
    }

    public boolean isRangeParticlesEnabled() {
        return rangeParticlesEnabled;
    }

    public void setRangeParticlesEnabled(boolean rangeParticlesEnabled) {
        this.rangeParticlesEnabled = rangeParticlesEnabled;
    }

    public String getRangeParticleType() {
        return rangeParticleType;
    }

    public void setRangeParticleType(String rangeParticleType) {
        this.rangeParticleType = rangeParticleType;
    }

    public String getBeamStyle() {
        return beamStyle;
    }

    public void setBeamStyle(String beamStyle) {
        this.beamStyle = beamStyle;
    }

    public boolean hasEffect(String effectId) {
        return effectLevels.containsKey(effectId);
    }

    public boolean isEffectActive(String effectId) {
        return activeEffects.contains(effectId);
    }

    public int getEffectLevel(String effectId) {
        Integer level = effectLevels.get(effectId);
        return level != null ? level : 0;
    }

    public void acquireEffect(String effectId) {
        if (!effectLevels.containsKey(effectId)) {
            effectLevels.put(effectId, 1);
        }
        activeEffects.add(effectId);
    }

    public void setEffectLevel(String effectId, int level) {
        if (level <= 0) {
            effectLevels.remove(effectId);
            activeEffects.remove(effectId);
            return;
        }
        effectLevels.put(effectId, level);
    }

    public void setEffectActive(String effectId, boolean active) {
        if (!hasEffect(effectId))
            return;

        if (active) {
            activeEffects.add(effectId);
        } else {
            activeEffects.remove(effectId);
        }
    }

    public Set<UUID> getTrustedPlayers() {
        return trustedPlayers;
    }

    public boolean isTrusted(UUID uuid) {
        return uuid != null && trustedPlayers.contains(uuid);
    }

    public void addTrusted(UUID uuid) {
        if (uuid != null)
            trustedPlayers.add(uuid);
    }

    public void removeTrusted(UUID uuid) {
        trustedPlayers.remove(uuid);
    }

    public boolean isProtectBaseBlocks() {
        return protectBaseBlocks;
    }

    public void setProtectBaseBlocks(boolean protectBaseBlocks) {
        this.protectBaseBlocks = protectBaseBlocks;
    }

    public boolean canManage(UUID uuid) {
        if (uuid == null)
            return false;
        return owner != null && owner.equals(uuid) || trustedPlayers.contains(uuid);
    }
}