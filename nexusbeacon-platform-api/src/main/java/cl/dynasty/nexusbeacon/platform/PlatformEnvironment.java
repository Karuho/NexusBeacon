package cl.dynasty.nexusbeacon.platform;

import org.bukkit.Server;

public final class PlatformEnvironment {
    private PlatformEnvironment() {}

    public static PlatformDescriptor detect(Server server) {
        return new PlatformDescriptor(
                MinecraftVersion.parse(server.getBukkitVersion()),
                JavaFeatureVersion.parse(System.getProperty("java.specification.version")),
                server.getName());
    }
}
