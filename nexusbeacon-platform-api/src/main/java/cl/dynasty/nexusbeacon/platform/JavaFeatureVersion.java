package cl.dynasty.nexusbeacon.platform;

import java.util.Objects;

public final class JavaFeatureVersion {
    private JavaFeatureVersion() {}

    public static int parse(String value) {
        Objects.requireNonNull(value, "value");
        String normalized = value.trim();
        if (normalized.startsWith("1.")) normalized = normalized.substring(2);
        int separator = normalized.indexOf('.');
        String feature = separator < 0 ? normalized : normalized.substring(0, separator);
        try {
            int parsed = Integer.parseInt(feature);
            if (parsed <= 0) throw new NumberFormatException();
            return parsed;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Unsupported Java feature version: " + value, exception);
        }
    }
}
