package cl.dynasty.nexusbeacon.beam;

import org.bukkit.Color;
import org.bukkit.Particle;

public final class BeamStyle {

    private final String id;
    private final String name;
    private final Particle particle;
    private final Color color;
    private final float size;
    private final String permission;

    public BeamStyle(String id, String name, Particle particle, Color color, float size, String permission) {
        this.id = id;
        this.name = name;
        this.particle = particle;
        this.color = color;
        this.size = size;
        this.permission = permission;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Particle getParticle() {
        return particle;
    }

    public Color getColor() {
        return color;
    }

    public float getSize() {
        return size;
    }

    public String getPermission() {
        return permission;
    }

    public boolean hasPermission() {
        return permission != null && !permission.isBlank();
    }
}