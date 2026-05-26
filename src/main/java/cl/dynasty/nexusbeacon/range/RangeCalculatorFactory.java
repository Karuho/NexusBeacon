package cl.dynasty.nexusbeacon.range;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.configuration.ConfigurationSection;

public final class RangeCalculatorFactory {

    private RangeCalculatorFactory() {
    }

    public static RangeCalculator create(ConfigurationSection config, int fallbackRange) {
        if (config == null || !config.getBoolean("enabled", false)) {
            return new FixedRangeCalculator(fallbackRange);
        }

        String mode = config.getString("mode", "FIXED").toUpperCase();

        if (mode.equals("PERCENTILE")) {
            return new PercentileRangeCalculator(
                    config.getInt("percentile.min-range", fallbackRange),
                    config.getInt("percentile.max-range", fallbackRange),
                    config.getDouble("percentile.percentage", 100.0D));
        }

        if (mode.equals("EXPONENTIAL")) {
            return new ExponentialRangeCalculator(
                    config.getInt("exponential.min-range", fallbackRange),
                    config.getInt("exponential.max-range", fallbackRange),
                    config.getDouble("exponential.exponent", 0.9D));
        }

        if (mode.equals("CLASSIFIED")) {
            Map<Integer, Integer> values = new HashMap<>();
            ConfigurationSection section = config.getConfigurationSection("classified");

            if (section != null) {
                for (String key : section.getKeys(false)) {
                    try {
                        values.put(Integer.parseInt(key), section.getInt(key));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }

            return new ClassifiedRangeCalculator(values);
        }

        return new FixedRangeCalculator(fallbackRange);
    }
}