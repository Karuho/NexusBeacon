package cl.dynasty.nexusbeacon.platform.legacy;

public final class LegacyParticleColor {
    private final int red;
    private final int green;
    private final int blue;

    public LegacyParticleColor(int red, int green, int blue) {
        validate(red, "red");
        validate(green, "green");
        validate(blue, "blue");
        this.red = red;
        this.green = green;
        this.blue = blue;
    }

    public int getRed() { return red; }
    public int getGreen() { return green; }
    public int getBlue() { return blue; }

    private static void validate(int value, String component) {
        if (value < 0 || value > 255) throw new IllegalArgumentException(component + " must be 0-255");
    }
}
