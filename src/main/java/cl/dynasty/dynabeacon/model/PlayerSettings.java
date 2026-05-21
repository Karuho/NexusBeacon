package cl.dynasty.dynabeacon.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerSettings {

    private final UUID uuid;
    private boolean showParticle;
    private boolean showAnimation;
    private final List<String> beaconInfoList;
    private String particleType;

    public PlayerSettings(UUID uuid) {
        this(uuid, true, true, new ArrayList<String>(), "VILLAGER_HAPPY");
    }

    public PlayerSettings(UUID uuid, boolean showParticle, boolean showAnimation, List<String> beaconInfoList,
            String particleType) {
        this.uuid = uuid;
        this.showParticle = showParticle;
        this.showAnimation = showAnimation;
        this.beaconInfoList = beaconInfoList != null ? beaconInfoList : new ArrayList<String>();
        this.particleType = particleType != null ? particleType : "VILLAGER_HAPPY";
    }

    public UUID getUuid() {
        return uuid;
    }

    public boolean isShowParticle() {
        return showParticle;
    }

    public void setShowParticle(boolean showParticle) {
        this.showParticle = showParticle;
    }

    public boolean isShowAnimation() {
        return showAnimation;
    }

    public void setShowAnimation(boolean showAnimation) {
        this.showAnimation = showAnimation;
    }

    public List<String> getBeaconInfoList() {
        return beaconInfoList;
    }

    public String getParticleType() {
        return particleType;
    }

    public void setParticleType(String particleType) {
        this.particleType = particleType;
    }

}