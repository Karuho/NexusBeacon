package cl.dynasty.nexusbeacon.platform.classic;

import cl.dynasty.nexusbeacon.platform.MinecraftVersion;

public final class ClassicCapabilities {
    public enum IdentityBackend { CUSTOM_ITEM_TAGS, PERSISTENT_DATA_CONTAINER }
    private static final MinecraftVersion FLOOR = MinecraftVersion.parse("1.13.2");
    private static final MinecraftVersion CEILING = MinecraftVersion.parse("1.20.4");
    private static final MinecraftVersion PDC_FLOOR = MinecraftVersion.parse("1.14");
    private final MinecraftVersion version;
    private final IdentityBackend identityBackend;

    private ClassicCapabilities(MinecraftVersion version, IdentityBackend identityBackend) {
        this.version = version;
        this.identityBackend = identityBackend;
    }

    public static ClassicCapabilities forVersion(MinecraftVersion version) {
        if (version == null) throw new NullPointerException("version");
        if (version.compareTo(FLOOR) < 0 || version.compareTo(CEILING) > 0) {
            throw new IllegalArgumentException("Unsupported Classic Minecraft version: " + version);
        }
        return new ClassicCapabilities(version, version.compareTo(PDC_FLOOR) < 0
                ? IdentityBackend.CUSTOM_ITEM_TAGS : IdentityBackend.PERSISTENT_DATA_CONTAINER);
    }

    public MinecraftVersion getVersion() { return version; }
    public IdentityBackend getIdentityBackend() { return identityBackend; }
    public boolean hasPersistentDataContainer() { return identityBackend == IdentityBackend.PERSISTENT_DATA_CONTAINER; }
    public boolean usesBukkitScheduler() { return true; }
    public boolean usesSynchronousTeleport() { return true; }
}
