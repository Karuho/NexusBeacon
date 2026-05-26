package cl.dynasty.nexusbeacon.range;

public class PercentileRangeCalculator implements RangeCalculator {

    private final int minRange;
    private final int maxRange;
    private final double percentage;

    public PercentileRangeCalculator(int minRange, int maxRange, double percentage) {
        this.minRange = minRange;
        this.maxRange = maxRange;
        this.percentage = percentage;
    }

    @Override
    public int calculate(int power) {
        int range = (int) Math.round(power * (percentage / 100.0D));
        return Math.max(minRange, Math.min(range, maxRange));
    }
}