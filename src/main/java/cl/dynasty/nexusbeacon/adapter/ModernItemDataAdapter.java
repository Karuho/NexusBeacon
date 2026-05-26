package cl.dynasty.nexusbeacon.adapter;

import cl.dynasty.nexusbeacon.NexusBeaconPlugin;
import cl.dynasty.nexusbeacon.api.ItemDataAdapter;
import cl.dynasty.nexusbeacon.model.BeaconData;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

public class ModernItemDataAdapter implements ItemDataAdapter {

    private final NexusBeaconPlugin plugin;

    public ModernItemDataAdapter(NexusBeaconPlugin plugin) {
        this.plugin = plugin;
    }

    public static boolean isSupported() {
        try {
            Class.forName("org.bukkit.persistence.PersistentDataType");
            Class.forName("org.bukkit.NamespacedKey");
            ItemMeta.class.getMethod("getPersistentDataContainer");
            return true;
        } catch (Exception exception) {
            return false;
        }
    }

    @Override
    public ItemStack writeBaseMarker(ItemStack item) {
        set(item, "NexusBeacon", "true");
        return item;
    }

    @Override
    public ItemStack writeBeaconData(ItemStack item, BeaconData beacon) {
        set(item, "NexusBeacon", "true");
        set(item, "uid", beacon.getUniqueId());
        set(item, "effects", serializeEffects(beacon.getEffectLevels()));
        set(item, "active", serializeActive(beacon.getActiveEffects()));
        return item;
    }

    @Override
    public boolean isCustomBeacon(ItemStack item) {
        return "true".equalsIgnoreCase(get(item, "NexusBeacon"));
    }

    @Override
    public String readUniqueId(ItemStack item) {
        String value = get(item, "uid");
        return value != null && !value.isEmpty() ? value : UUID.randomUUID().toString();
    }

    @Override
    public Map<String, Integer> readEffects(ItemStack item) {
        return deserializeEffects(get(item, "effects"));
    }

    @Override
    public Set<String> readActiveEffects(ItemStack item) {
        return deserializeActive(get(item, "active"));
    }

    private void set(ItemStack item, String keyName, String value) {
        try {
            ItemMeta meta = item.getItemMeta();
            if (meta == null) return;

            Object container = meta.getClass().getMethod("getPersistentDataContainer").invoke(meta);
            Object key = createKey(keyName);
            Object stringType = getStringType();

            Method setMethod = findMethod(container.getClass(), "set", 3);
            setMethod.invoke(container, key, stringType, value);

            item.setItemMeta(meta);
        } catch (Exception ignored) {
        }
    }

    private String get(ItemStack item, String keyName) {
        try {
            if (item == null || !item.hasItemMeta()) return null;

            ItemMeta meta = item.getItemMeta();
            if (meta == null) return null;

            Object container = meta.getClass().getMethod("getPersistentDataContainer").invoke(meta);
            Object key = createKey(keyName);
            Object stringType = getStringType();

            Method getMethod = findMethod(container.getClass(), "get", 2);
            Object value = getMethod.invoke(container, key, stringType);

            return value != null ? value.toString() : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private Object createKey(String keyName) throws Exception {
        Class<?> keyClass = Class.forName("org.bukkit.NamespacedKey");
        Constructor<?> constructor = keyClass.getConstructor(org.bukkit.plugin.Plugin.class, String.class);
        return constructor.newInstance(plugin, keyName);
    }

    private Object getStringType() throws Exception {
        Class<?> dataTypeClass = Class.forName("org.bukkit.persistence.PersistentDataType");
        Field field = dataTypeClass.getField("STRING");
        return field.get(null);
    }

    private Method findMethod(Class<?> clazz, String name, int params) {
        for (Method method : clazz.getMethods()) {
            if (method.getName().equals(name) && method.getParameterTypes().length == params) {
                return method;
            }
        }
        throw new IllegalStateException("Método no encontrado: " + name);
    }

    private String serializeEffects(Map<String, Integer> effects) {
        StringBuilder builder = new StringBuilder();

        for (Map.Entry<String, Integer> entry : effects.entrySet()) {
            if (builder.length() > 0) builder.append(";");
            builder.append(entry.getKey()).append(":").append(entry.getValue());
        }

        return builder.toString();
    }

    private Map<String, Integer> deserializeEffects(String raw) {
        Map<String, Integer> effects = new HashMap<String, Integer>();
        if (raw == null || raw.isEmpty()) return effects;

        for (String part : raw.split(";")) {
            String[] split = part.split(":");
            if (split.length != 2) continue;

            try {
                effects.put(split[0].toLowerCase(), Integer.parseInt(split[1]));
            } catch (NumberFormatException ignored) {
            }
        }

        return effects;
    }

    private String serializeActive(Set<String> active) {
        StringBuilder builder = new StringBuilder();

        for (String effect : active) {
            if (builder.length() > 0) builder.append(";");
            builder.append(effect);
        }

        return builder.toString();
    }

    private Set<String> deserializeActive(String raw) {
        Set<String> active = new HashSet<String>();
        if (raw == null || raw.isEmpty()) return active;

        for (String part : raw.split(";")) {
            active.add(part.toLowerCase());
        }

        return active;
    }
}