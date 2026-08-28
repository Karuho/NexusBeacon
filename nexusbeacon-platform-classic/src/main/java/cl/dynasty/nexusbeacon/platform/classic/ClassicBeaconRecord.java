package cl.dynasty.nexusbeacon.platform.classic;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;

public final class ClassicBeaconRecord {
    private final ClassicBeaconLocation location; private final UUID uniqueId; private final UUID owner;
    private final int range; private final int level; private final Map<String,Integer> effects; private final Set<String> active;
    private final Set<UUID> trusted; private final boolean protectBase; private final String beamStyle;
    private final boolean rangeParticles; private final String rangeParticleType;
    public ClassicBeaconRecord(ClassicBeaconLocation location, UUID uniqueId, UUID owner, int range, int level,
            Map<String,Integer> effects, Set<String> active, Set<UUID> trusted, boolean protectBase,
            String beamStyle, boolean rangeParticles, String rangeParticleType) {
        if (location == null || uniqueId == null || range <= 0 || level <= 0) throw new IllegalArgumentException("Invalid beacon record");
        this.location=location; this.uniqueId=uniqueId; this.owner=owner; this.range=range; this.level=level;
        this.effects=Collections.unmodifiableMap(new TreeMap<String,Integer>(effects == null ? Collections.<String,Integer>emptyMap() : effects));
        this.active=Collections.unmodifiableSet(new TreeSet<String>(active == null ? Collections.<String>emptySet() : active));
        if (!this.effects.keySet().containsAll(this.active)) throw new IllegalArgumentException("Active effect was not acquired");
        this.trusted=Collections.unmodifiableSet(new TreeSet<UUID>(trusted == null ? Collections.<UUID>emptySet() : trusted));
        this.protectBase=protectBase; this.beamStyle=beamStyle; this.rangeParticles=rangeParticles;
        this.rangeParticleType=rangeParticleType == null ? "VILLAGER_HAPPY" : rangeParticleType;
    }
    public ClassicBeaconLocation getLocation(){return location;} public UUID getUniqueId(){return uniqueId;} public UUID getOwner(){return owner;}
    public int getRange(){return range;} public int getLevel(){return level;} public Map<String,Integer> getEffects(){return effects;}
    public Set<String> getActiveEffects(){return active;} public Set<UUID> getTrusted(){return trusted;} public boolean isProtectBase(){return protectBase;}
    public String getBeamStyle(){return beamStyle;} public boolean isRangeParticles(){return rangeParticles;} public String getRangeParticleType(){return rangeParticleType;}
    public ClassicBeaconRecord withTrusted(Set<UUID> value){return new ClassicBeaconRecord(location,uniqueId,owner,range,level,effects,active,value,protectBase,beamStyle,rangeParticles,rangeParticleType);}
    public ClassicBeaconRecord withVisuals(String style, boolean enabled, String particle){return new ClassicBeaconRecord(location,uniqueId,owner,range,level,effects,active,trusted,protectBase,style,enabled,particle);}
    @Override public boolean equals(Object o){if(!(o instanceof ClassicBeaconRecord))return false; ClassicBeaconRecord b=(ClassicBeaconRecord)o; return location.equals(b.location)&&uniqueId.equals(b.uniqueId)&&eq(owner,b.owner)&&range==b.range&&level==b.level&&effects.equals(b.effects)&&active.equals(b.active)&&trusted.equals(b.trusted)&&protectBase==b.protectBase&&eq(beamStyle,b.beamStyle)&&rangeParticles==b.rangeParticles&&rangeParticleType.equals(b.rangeParticleType);}
    @Override public int hashCode(){return uniqueId.hashCode()*31+location.hashCode();} private static boolean eq(Object a,Object b){return a==null?b==null:a.equals(b);}
}
