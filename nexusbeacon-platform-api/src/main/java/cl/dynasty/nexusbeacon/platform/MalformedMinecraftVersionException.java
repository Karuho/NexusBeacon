package cl.dynasty.nexusbeacon.platform;

public final class MalformedMinecraftVersionException extends IllegalArgumentException {
    public MalformedMinecraftVersionException(String value) {
        super("Malformed Minecraft version: " + value);
    }
}
