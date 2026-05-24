package cl.dynasty.dynabeacon.range;

public class FixedRangeCalculator implements RangeCalculator {

    private final int range;

    public FixedRangeCalculator(int range) {
        this.range = range;
    }

    @Override
    public int calculate(int power) {
        return range;
    }
}