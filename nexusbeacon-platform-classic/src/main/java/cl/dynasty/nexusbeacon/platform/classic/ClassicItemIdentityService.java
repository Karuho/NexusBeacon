package cl.dynasty.nexusbeacon.platform.classic;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import cl.dynasty.nexusbeacon.platform.api.IdentificationResult;
import cl.dynasty.nexusbeacon.platform.api.ItemIdentity;
import cl.dynasty.nexusbeacon.platform.api.ItemIdentityService;

public final class ClassicItemIdentityService implements ItemIdentityService {
    interface MarkerAccess { Boolean read(ItemStack item); ItemStack write(ItemStack item); }
    private final MarkerAccess access;

    public ClassicItemIdentityService(Plugin plugin, ClassicCapabilities capabilities) {
        if (plugin == null) throw new NullPointerException("plugin");
        if (capabilities == null) throw new NullPointerException("capabilities");
        NamespacedKey key = new NamespacedKey(plugin, "nexus_beacon");
        this.access = capabilities.hasPersistentDataContainer() ? new ReflectivePdcAccess(key) : new ReflectiveCustomTagAccess(key);
    }
    ClassicItemIdentityService(MarkerAccess access) { if (access == null) throw new NullPointerException("access"); this.access = access; }

    public IdentificationResult identify(ItemStack item) {
        if (item == null) return IdentificationResult.notRecognized();
        Boolean marker = access.read(item);
        if (Boolean.TRUE.equals(marker)) return IdentificationResult.recognized(ItemIdentity.NEXUS_BEACON, IdentificationResult.Evidence.PERSISTENT_MARKER);
        if (Boolean.FALSE.equals(marker)) return IdentificationResult.malformed();
        return IdentificationResult.notRecognized();
    }
    public ItemStack mark(ItemStack item, ItemIdentity identity) {
        if (item == null) throw new NullPointerException("item");
        if (identity != ItemIdentity.NEXUS_BEACON) throw new IllegalArgumentException("Unsupported identity: " + identity);
        return access.write(item);
    }

    private abstract static class ReflectiveAccess implements MarkerAccess {
        final NamespacedKey key; final String containerMethod; final String typeClass;
        ReflectiveAccess(NamespacedKey key, String containerMethod, String typeClass) { this.key = key; this.containerMethod = containerMethod; this.typeClass = typeClass; }
        public Boolean read(ItemStack item) { try {
            ItemMeta meta = item.getItemMeta(); if (meta == null) return null;
            Object container = meta.getClass().getMethod(containerMethod).invoke(meta);
            Class<?> types = Class.forName(typeClass); Field byteField = types.getField("BYTE"); Object byteType = byteField.get(null);
            Method has = find(container.getClass(), hasMethod(), 2); if (!((Boolean) has.invoke(container, key, byteType)).booleanValue()) return null;
            Method get = find(container.getClass(), getMethod(), 2); Object value = get.invoke(container, key, byteType);
            return value instanceof Byte && ((Byte) value).byteValue() == 1 ? Boolean.TRUE : Boolean.FALSE;
        } catch (ReflectiveOperationException failure) { throw new IllegalStateException("Classic identity backend unavailable", failure); } }
        public ItemStack write(ItemStack item) { try {
            ItemStack copy = item.clone(); ItemMeta meta = copy.getItemMeta(); if (meta == null) throw new IllegalArgumentException("Item has no metadata");
            Object container = meta.getClass().getMethod(containerMethod).invoke(meta);
            Class<?> types = Class.forName(typeClass); Object byteType = types.getField("BYTE").get(null);
            find(container.getClass(), setMethod(), 3).invoke(container, key, byteType, Byte.valueOf((byte) 1));
            copy.setItemMeta(meta); return copy;
        } catch (ReflectiveOperationException failure) { throw new IllegalStateException("Classic identity backend unavailable", failure); } }
        private static Method find(Class<?> type, String name, int count) throws NoSuchMethodException {
            for (Method method : type.getMethods()) if (method.getName().equals(name) && method.getParameterTypes().length == count) return method;
            throw new NoSuchMethodException(type.getName() + "." + name);
        }
        abstract String hasMethod(); abstract String getMethod(); abstract String setMethod();
    }
    private static final class ReflectiveCustomTagAccess extends ReflectiveAccess {
        ReflectiveCustomTagAccess(NamespacedKey key) { super(key, "getCustomTagContainer", "org.bukkit.inventory.meta.tags.ItemTagType"); }
        String hasMethod() { return "hasCustomTag"; } String getMethod() { return "getCustomTag"; } String setMethod() { return "setCustomTag"; }
    }
    private static final class ReflectivePdcAccess extends ReflectiveAccess {
        ReflectivePdcAccess(NamespacedKey key) { super(key, "getPersistentDataContainer", "org.bukkit.persistence.PersistentDataType"); }
        String hasMethod() { return "has"; } String getMethod() { return "get"; } String setMethod() { return "set"; }
    }
}
