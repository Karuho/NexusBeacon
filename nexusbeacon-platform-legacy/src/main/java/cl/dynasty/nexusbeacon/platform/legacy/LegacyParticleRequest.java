package cl.dynasty.nexusbeacon.platform.legacy;

import org.bukkit.Location;

public final class LegacyParticleRequest {
    private final String particleName;
    private final Location location;
    private final int amount;
    private final double offsetX;
    private final double offsetY;
    private final double offsetZ;
    private final double speed;
    private final LegacyParticleColor color;
    private final float size;

    public LegacyParticleRequest(String particleName, Location location, int amount,
            double offsetX, double offsetY, double offsetZ, double speed,
            LegacyParticleColor color, float size) {
        if (location == null) throw new NullPointerException("location");
        if (amount < 1) throw new IllegalArgumentException("amount must be at least 1");
        if (size <= 0.0F) throw new IllegalArgumentException("size must be positive");
        this.particleName = particleName;
        this.location = location.clone();
        this.amount = amount;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        this.speed = speed;
        this.color = color;
        this.size = size;
    }

    public String getParticleName() { return particleName; }
    public Location getLocation() { return location.clone(); }
    public int getAmount() { return amount; }
    public double getOffsetX() { return offsetX; }
    public double getOffsetY() { return offsetY; }
    public double getOffsetZ() { return offsetZ; }
    public double getSpeed() { return speed; }
    public LegacyParticleColor getColor() { return color; }
    public float getSize() { return size; }
}
