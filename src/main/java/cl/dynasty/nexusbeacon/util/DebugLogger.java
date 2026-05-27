package cl.dynasty.nexusbeacon.util;

import java.util.HashMap;
import java.util.Map;

import cl.dynasty.nexusbeacon.NexusBeaconPlugin;

public final class DebugLogger {

    private static final Map<String, Long> LAST_LOGS = new HashMap<>();

    private DebugLogger() {
    }

    public static void log(NexusBeaconPlugin plugin, String key, String message) {
        log(plugin, key, message, 10000L);
    }

    public static void log(NexusBeaconPlugin plugin, String key, String message, long cooldownMillis) {
        if (plugin == null || !plugin.isDebugEnabled()) {
            return;
        }

        long now = System.currentTimeMillis();
        long last = LAST_LOGS.getOrDefault(key, 0L);

        if (now - last < cooldownMillis) {
            return;
        }

        LAST_LOGS.put(key, now);
        plugin.getLogger().info("[Debug] " + message);
    }
}