package cl.dynasty.nexusbeacon.platform.legacy;

public enum LegacyParticleRuntime {
    SPIGOT_1_8("v1_8_R3", false),
    SPIGOT_1_12("v1_12_R1", true);

    private final String revision;
    private final boolean bukkitParticles;

    LegacyParticleRuntime(String revision, boolean bukkitParticles) {
        this.revision = revision;
        this.bukkitParticles = bukkitParticles;
    }

    public String getRevision() { return revision; }
    public boolean hasBukkitParticles() { return bukkitParticles; }

    public static LegacyParticleRuntime fromCraftPackage(String craftPackage) {
        if (craftPackage != null && craftPackage.contains(".v1_8_R3")) return SPIGOT_1_8;
        if (craftPackage != null && craftPackage.contains(".v1_12_R1")) return SPIGOT_1_12;
        throw new IllegalArgumentException("Unsupported Legacy particle runtime: " + craftPackage);
    }
}
