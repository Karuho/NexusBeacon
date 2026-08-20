package cl.dynasty.nexusbeacon.platform;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MinecraftVersion implements Comparable<MinecraftVersion> {
    private static final Pattern VERSION_PREFIX = Pattern.compile("^(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?.*$");

    private final int major;
    private final int minor;
    private final int patch;

    private MinecraftVersion(int major, int minor, int patch) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
    }

    public static MinecraftVersion parse(String value) {
        Objects.requireNonNull(value, "value");
        Matcher matcher = VERSION_PREFIX.matcher(value.trim());
        if (!matcher.matches()) {
            throw new MalformedMinecraftVersionException(value);
        }
        return new MinecraftVersion(parsePart(matcher.group(1)), parsePart(matcher.group(2)), parsePart(matcher.group(3)));
    }

    private static int parsePart(String value) {
        return value == null ? 0 : Integer.parseInt(value);
    }

    public int getMajor() { return major; }
    public int getMinor() { return minor; }
    public int getPatch() { return patch; }

    @Override
    public int compareTo(MinecraftVersion other) {
        int result = Integer.compare(major, other.major);
        if (result == 0) result = Integer.compare(minor, other.minor);
        if (result == 0) result = Integer.compare(patch, other.patch);
        return result;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof MinecraftVersion)) return false;
        MinecraftVersion other = (MinecraftVersion) object;
        return major == other.major && minor == other.minor && patch == other.patch;
    }

    @Override
    public int hashCode() { return Objects.hash(major, minor, patch); }

    @Override
    public String toString() { return major + "." + minor + "." + patch; }
}
