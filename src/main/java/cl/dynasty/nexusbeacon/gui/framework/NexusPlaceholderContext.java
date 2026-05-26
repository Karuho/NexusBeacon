package cl.dynasty.nexusbeacon.gui.framework;

import java.util.HashMap;
import java.util.Map;

public final class NexusPlaceholderContext {

    private final Map<String, String> placeholders = new HashMap<>();

    public NexusPlaceholderContext put(String key, Object value) {
        placeholders.put(key, String.valueOf(value));
        return this;
    }

    public String apply(String text) {
        if (text == null) {
            return "";
        }

        String result = text;

        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace("%" + entry.getKey() + "%", entry.getValue());
        }

        return result;
    }
}