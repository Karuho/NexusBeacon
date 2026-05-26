package cl.dynasty.nexusbeacon.range;

public class ExponentialRangeCalculator implements RangeCalculator {

    private final int minRange;
    private final int maxRange;
    private final double exponent;

    public ExponentialRangeCalculator(int minRange, int maxRange, double exponent) {
        this.minRange = minRange;
        this.maxRange = maxRange;
        this.exponent = exponent;
    }

    @Override
    public int calculate(int power) {
        int range = (int) Math.round(Math.pow(power, exponent));
        return Math.max(minRange, Math.min(range, maxRange));
    }
}