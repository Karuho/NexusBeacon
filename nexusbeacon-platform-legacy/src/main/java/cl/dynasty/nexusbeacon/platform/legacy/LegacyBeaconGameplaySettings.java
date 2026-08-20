package cl.dynasty.nexusbeacon.platform.legacy;

/** Immutable settings used by the transactional block subset. */
public final class LegacyBeaconGameplaySettings {
    private final int defaultRange;
    private final boolean protectBaseBlocks;
    private final boolean rangeParticlesEnabled;
    private final String rangeParticleType;
    private final boolean ownerOnlyBreak;
    private final boolean creativeNoDupe;
    private final boolean cancelIfInventoryFull;
    private final boolean autoPickup;

    public LegacyBeaconGameplaySettings(int defaultRange, boolean protectBaseBlocks,
            boolean rangeParticlesEnabled, String rangeParticleType, boolean ownerOnlyBreak,
            boolean creativeNoDupe, boolean cancelIfInventoryFull, boolean autoPickup) {
        if (defaultRange <= 0) throw new IllegalArgumentException("defaultRange must be positive");
        if (rangeParticleType == null || rangeParticleType.trim().isEmpty()) {
            throw new IllegalArgumentException("rangeParticleType is required");
        }
        this.defaultRange = defaultRange;
        this.protectBaseBlocks = protectBaseBlocks;
        this.rangeParticlesEnabled = rangeParticlesEnabled;
        this.rangeParticleType = rangeParticleType;
        this.ownerOnlyBreak = ownerOnlyBreak;
        this.creativeNoDupe = creativeNoDupe;
        this.cancelIfInventoryFull = cancelIfInventoryFull;
        this.autoPickup = autoPickup;
    }

    public int getDefaultRange() { return defaultRange; }
    public boolean isProtectBaseBlocks() { return protectBaseBlocks; }
    public boolean isRangeParticlesEnabled() { return rangeParticlesEnabled; }
    public String getRangeParticleType() { return rangeParticleType; }
    public boolean isOwnerOnlyBreak() { return ownerOnlyBreak; }
    public boolean isCreativeNoDupe() { return creativeNoDupe; }
    public boolean isCancelIfInventoryFull() { return cancelIfInventoryFull; }
    public boolean isAutoPickup() { return autoPickup; }
}
