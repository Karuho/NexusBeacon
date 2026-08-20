package cl.dynasty.nexusbeacon.platform.legacy;

import java.util.Collections;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.potion.PotionEffectType;

public final class LegacyEffectDefinition {
    private final String id;
    private final String type;
    private final int maxLevel;
    private final String target;
    private final PotionEffectType potion;
    private final int potionDurationTicks;
    private final int amplifierPerLevel;
    private final List<Material> targetBlocks;
    private final boolean supported;
    private final String diagnostic;

    LegacyEffectDefinition(String id, String type, int maxLevel, String target, PotionEffectType potion,
            int potionDurationTicks, int amplifierPerLevel, List<Material> targetBlocks,
            boolean supported, String diagnostic) {
        this.id = id;
        this.type = type;
        this.maxLevel = maxLevel;
        this.target = target;
        this.potion = potion;
        this.potionDurationTicks = potionDurationTicks;
        this.amplifierPerLevel = amplifierPerLevel;
        this.targetBlocks = Collections.unmodifiableList(targetBlocks);
        this.supported = supported;
        this.diagnostic = diagnostic;
    }

    public String getId() { return id; }
    public String getType() { return type; }
    public int getMaxLevel() { return maxLevel; }
    public String getTarget() { return target; }
    public PotionEffectType getPotion() { return potion; }
    public int getPotionDurationTicks() { return potionDurationTicks; }
    public int getAmplifierPerLevel() { return amplifierPerLevel; }
    public List<Material> getTargetBlocks() { return targetBlocks; }
    public boolean isSupported() { return supported; }
    public String getDiagnostic() { return diagnostic; }
}
