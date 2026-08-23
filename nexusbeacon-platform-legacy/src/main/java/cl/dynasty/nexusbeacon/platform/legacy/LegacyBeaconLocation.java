package cl.dynasty.nexusbeacon.platform.legacy;

/** Stable block identity which does not require a Bukkit World to be loaded. */
public final class LegacyBeaconLocation implements Comparable<LegacyBeaconLocation> {
    private static final int MAX_HORIZONTAL_COORDINATE = 30000000;
    private final String worldName;
    private final int x;
    private final int y;
    private final int z;

    public LegacyBeaconLocation(String worldName, int x, int y, int z) {
        if (worldName == null || worldName.isEmpty() || !worldName.equals(worldName.trim())
                || worldName.indexOf(';') >= 0 || worldName.indexOf('.') >= 0
                || containsControlCharacter(worldName)) {
            throw new IllegalArgumentException("Invalid world name");
        }
        if (Math.abs((long) x) > MAX_HORIZONTAL_COORDINATE
                || Math.abs((long) z) > MAX_HORIZONTAL_COORDINATE || y < 0 || y > 255) {
            throw new IllegalArgumentException("Invalid Legacy block coordinates");
        }
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static LegacyBeaconLocation parse(String value) {
        if (value == null) throw new IllegalArgumentException("Location is missing");
        String[] parts = value.split(";", -1);
        if (parts.length != 4) throw new IllegalArgumentException("Location must contain world;x;y;z");
        try {
            return new LegacyBeaconLocation(parts[0], Integer.parseInt(parts[1]),
                    Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Location coordinates must be integers", exception);
        }
    }

    public String toStorageKey() { return worldName + ";" + x + ";" + y + ";" + z; }
    public String getWorldName() { return worldName; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getZ() { return z; }

    @Override public int compareTo(LegacyBeaconLocation other) {
        if (other == null) return 1;
        return toStorageKey().compareTo(other.toStorageKey());
    }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof LegacyBeaconLocation)) return false;
        LegacyBeaconLocation location = (LegacyBeaconLocation) other;
        return x == location.x && y == location.y && z == location.z
                && worldName.equals(location.worldName);
    }

    @Override public int hashCode() {
        int result = worldName.hashCode();
        result = 31 * result + x;
        result = 31 * result + y;
        return 31 * result + z;
    }

    @Override public String toString() { return toStorageKey(); }

    private static boolean containsControlCharacter(String value) {
        for (int index = 0; index < value.length(); index++) {
            if (Character.isISOControl(value.charAt(index))) return true;
        }
        return false;
    }
}
