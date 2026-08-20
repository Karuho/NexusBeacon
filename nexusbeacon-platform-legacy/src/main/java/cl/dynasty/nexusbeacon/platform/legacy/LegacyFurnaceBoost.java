package cl.dynasty.nexusbeacon.platform.legacy;

public final class LegacyFurnaceBoost {
    private final double cookPercent;
    private final double fuelPercent;

    LegacyFurnaceBoost(double cookPercent, double fuelPercent) {
        this.cookPercent = cookPercent;
        this.fuelPercent = fuelPercent;
    }

    public double getCookPercent() { return cookPercent; }
    public double getFuelPercent() { return fuelPercent; }
}
