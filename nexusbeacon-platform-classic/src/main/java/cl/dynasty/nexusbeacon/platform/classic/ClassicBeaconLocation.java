package cl.dynasty.nexusbeacon.platform.classic;

public final class ClassicBeaconLocation implements Comparable<ClassicBeaconLocation> {
    private final String world; private final int x; private final int y; private final int z;
    public ClassicBeaconLocation(String world, int x, int y, int z) {
        if (world == null || world.trim().isEmpty() || !world.equals(world.trim()) || world.contains(";")) throw new IllegalArgumentException("Invalid world");
        this.world = world; this.x = x; this.y = y; this.z = z;
    }
    public static ClassicBeaconLocation parse(String value) {
        String[] parts = value == null ? new String[0] : value.split(";", -1);
        if (parts.length != 4) throw new IllegalArgumentException("Invalid beacon location");
        try { return new ClassicBeaconLocation(parts[0], Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3])); }
        catch (NumberFormatException bad) { throw new IllegalArgumentException("Invalid beacon coordinates", bad); }
    }
    public String key() { return world + ";" + x + ";" + y + ";" + z; }
    public String getWorld() { return world; } public int getX() { return x; } public int getY() { return y; } public int getZ() { return z; }
    public int compareTo(ClassicBeaconLocation other) { return key().compareTo(other.key()); }
    @Override public boolean equals(Object other) { return other instanceof ClassicBeaconLocation && key().equals(((ClassicBeaconLocation) other).key()); }
    @Override public int hashCode() { return key().hashCode(); }
}
