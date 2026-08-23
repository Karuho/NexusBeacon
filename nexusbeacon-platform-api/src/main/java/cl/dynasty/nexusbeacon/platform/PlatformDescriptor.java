package cl.dynasty.nexusbeacon.platform;

import java.util.Objects;

public final class PlatformDescriptor {
    private final MinecraftVersion minecraftVersion;
    private final int javaFeatureVersion;
    private final String serverImplementation;

    public PlatformDescriptor(MinecraftVersion minecraftVersion, int javaFeatureVersion, String serverImplementation) {
        this.minecraftVersion = Objects.requireNonNull(minecraftVersion, "minecraftVersion");
        if (javaFeatureVersion <= 0) throw new IllegalArgumentException("javaFeatureVersion must be positive");
        this.javaFeatureVersion = javaFeatureVersion;
        this.serverImplementation = Objects.requireNonNull(serverImplementation, "serverImplementation");
    }

    public MinecraftVersion getMinecraftVersion() { return minecraftVersion; }
    public int getJavaFeatureVersion() { return javaFeatureVersion; }
    public String getServerImplementation() { return serverImplementation; }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof PlatformDescriptor)) return false;
        PlatformDescriptor other = (PlatformDescriptor) object;
        return javaFeatureVersion == other.javaFeatureVersion
                && minecraftVersion.equals(other.minecraftVersion)
                && serverImplementation.equals(other.serverImplementation);
    }

    @Override
    public int hashCode() { return Objects.hash(minecraftVersion, javaFeatureVersion, serverImplementation); }
}
