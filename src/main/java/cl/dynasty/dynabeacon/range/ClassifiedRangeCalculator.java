package cl.dynasty.dynabeacon.range;

import java.util.Map;
import java.util.TreeMap;

public class ClassifiedRangeCalculator implements RangeCalculator {

    private final TreeMap<Integer, Integer> ranges = new TreeMap<>();

    public ClassifiedRangeCalculator(Map<Integer, Integer> values) {
        if (values != null) {
            ranges.putAll(values);
        }
    }

    @Override
    public int calculate(int power) {
        Map.Entry<Integer, Integer> entry = ranges.floorEntry(power);

        if (entry == null) {
            return 0;
        }

        return entry.getValue();
    }
}