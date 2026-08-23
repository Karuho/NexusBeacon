package cl.dynasty.nexusbeacon.platform.legacy;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.bukkit.inventory.ItemStack;

public abstract class ReflectiveLegacyNbtBridge implements LegacyNbtBridge {
    private final String revision;
    private final Method asNmsCopy;
    private final Method asBukkitCopy;
    private final Method getTag;
    private final Method setTag;
    private final Constructor<?> compoundConstructor;
    private final Method compoundSet;
    private final Method compoundGet;
    private final Method compoundSetByte;
    private final Method compoundGetByte;
    private final Method compoundSetString;
    private final Method compoundGetString;

    protected ReflectiveLegacyNbtBridge(String revision) {
        this.revision = revision;
        try {
            Class<?> craftItem = Class.forName("org.bukkit.craftbukkit." + revision + ".inventory.CraftItemStack");
            Class<?> nmsItem = Class.forName("net.minecraft.server." + revision + ".ItemStack");
            Class<?> nbtBase = Class.forName("net.minecraft.server." + revision + ".NBTBase");
            Class<?> compound = Class.forName("net.minecraft.server." + revision + ".NBTTagCompound");
            asNmsCopy = craftItem.getMethod("asNMSCopy", ItemStack.class);
            asBukkitCopy = craftItem.getMethod("asBukkitCopy", nmsItem);
            getTag = nmsItem.getMethod("getTag");
            setTag = nmsItem.getMethod("setTag", compound);
            compoundConstructor = compound.getConstructor();
            compoundSet = compound.getMethod("set", String.class, nbtBase);
            compoundGet = compound.getMethod("get", String.class);
            compoundSetByte = compound.getMethod("setByte", String.class, byte.class);
            compoundGetByte = compound.getMethod("getByte", String.class);
            compoundSetString = compound.getMethod("setString", String.class, String.class);
            compoundGetString = compound.getMethod("getString", String.class);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot initialize Legacy NBT bridge " + revision, exception);
        }
    }

    @Override
    public ItemStack mark(ItemStack item) {
        if (item == null) throw new IllegalArgumentException("item");
        try {
            Object nms = asNmsCopy.invoke(null, item);
            Object root = getTag.invoke(nms);
            if (root == null) root = compoundConstructor.newInstance();
            Object identity = compoundConstructor.newInstance();
            compoundSetByte.invoke(identity, "item", Byte.valueOf((byte) 1));
            compoundSet.invoke(root, "NexusBeacon", identity);
            setTag.invoke(nms, root);
            return (ItemStack) asBukkitCopy.invoke(null, nms);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot mark Legacy item", exception);
        }
    }

    @Override
    public LegacyIdentityStatus identify(ItemStack item) {
        if (item == null) return LegacyIdentityStatus.NOT_RECOGNIZED;
        try {
            Object nms = asNmsCopy.invoke(null, item);
            Object root = getTag.invoke(nms);
            if (root == null) return LegacyIdentityStatus.NOT_RECOGNIZED;
            Object identity = compoundGet.invoke(root, "NexusBeacon");
            if (identity == null) return LegacyIdentityStatus.NOT_RECOGNIZED;
            if (!"NBTTagCompound".equals(identity.getClass().getSimpleName())) return LegacyIdentityStatus.MALFORMED;
            Object value = compoundGet.invoke(identity, "item");
            if (value == null || !"NBTTagByte".equals(value.getClass().getSimpleName())) return LegacyIdentityStatus.MALFORMED;
            byte marker = ((Byte) compoundGetByte.invoke(identity, "item")).byteValue();
            return marker == 1 ? LegacyIdentityStatus.RECOGNIZED : LegacyIdentityStatus.UNSUPPORTED;
        } catch (ReflectiveOperationException exception) {
            return LegacyIdentityStatus.MALFORMED;
        }
    }

    @Override
    public ItemStack writePortableData(ItemStack item, LegacyPortableBeaconData data) {
        if (item == null || data == null) throw new IllegalArgumentException("item and data are required");
        try {
            Object nms = asNmsCopy.invoke(null, item);
            Object root = getTag.invoke(nms);
            if (root == null) root = compoundConstructor.newInstance();
            Object identity = compoundGet.invoke(root, "NexusBeacon");
            if (identity == null || !"NBTTagCompound".equals(identity.getClass().getSimpleName())) {
                identity = compoundConstructor.newInstance();
            }
            compoundSetByte.invoke(identity, "item", Byte.valueOf((byte) 1));
            compoundSetString.invoke(identity, "uid", data.getUniqueId().toString());
            compoundSetString.invoke(identity, "effects", encodeEffects(data.getEffectLevels()));
            compoundSetString.invoke(identity, "active", encodeActive(data.getActiveEffects()));
            compoundSet.invoke(root, "NexusBeacon", identity);
            setTag.invoke(nms, root);
            return (ItemStack) asBukkitCopy.invoke(null, nms);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot write Legacy portable beacon data", exception);
        }
    }

    @Override
    public Optional<LegacyPortableBeaconData> readPortableData(ItemStack item) {
        if (identify(item) != LegacyIdentityStatus.RECOGNIZED) return Optional.empty();
        try {
            Object nms = asNmsCopy.invoke(null, item);
            Object root = getTag.invoke(nms);
            Object identity = compoundGet.invoke(root, "NexusBeacon");
            String uid = (String) compoundGetString.invoke(identity, "uid");
            if (uid == null || uid.isEmpty()) return Optional.empty();
            String effects = (String) compoundGetString.invoke(identity, "effects");
            String active = (String) compoundGetString.invoke(identity, "active");
            return Optional.of(new LegacyPortableBeaconData(UUID.fromString(uid),
                    decodeEffects(effects), decodeActive(active)));
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Malformed Legacy portable beacon data", exception);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalArgumentException("Cannot read Legacy portable beacon data", exception);
        }
    }

    private static String encodeEffects(Map<String, Integer> effects) {
        StringBuilder encoded = new StringBuilder();
        for (Map.Entry<String, Integer> entry : effects.entrySet()) {
            if (encoded.length() > 0) encoded.append(';');
            encoded.append(entry.getKey()).append(':').append(entry.getValue());
        }
        return encoded.toString();
    }

    private static String encodeActive(Set<String> active) {
        StringBuilder encoded = new StringBuilder();
        for (String effect : active) {
            if (encoded.length() > 0) encoded.append(';');
            encoded.append(effect);
        }
        return encoded.toString();
    }

    private static Map<String, Integer> decodeEffects(String encoded) {
        Map<String, Integer> effects = new LinkedHashMap<String, Integer>();
        if (encoded == null || encoded.isEmpty()) return effects;
        for (String part : encoded.split(";", -1)) {
            String[] pair = part.split(":", -1);
            if (pair.length != 2 || pair[0].isEmpty()) throw new IllegalArgumentException("Malformed effect data");
            int level = Integer.parseInt(pair[1]);
            if (level <= 0 || effects.put(pair[0], Integer.valueOf(level)) != null) {
                throw new IllegalArgumentException("Malformed effect data");
            }
        }
        return effects;
    }

    private static Set<String> decodeActive(String encoded) {
        Set<String> active = new LinkedHashSet<String>();
        if (encoded == null || encoded.isEmpty()) return active;
        for (String part : encoded.split(";", -1)) {
            if (part.isEmpty() || !active.add(part)) throw new IllegalArgumentException("Malformed active data");
        }
        return active;
    }

    @Override
    public String getRevision() { return revision; }
}
