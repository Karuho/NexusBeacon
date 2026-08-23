package cl.dynasty.nexusbeacon.platform.legacy;

public final class LegacyNbtBridgeFactory {
    private LegacyNbtBridgeFactory() { }

    public static LegacyNbtBridge create(String craftBukkitPackage) {
        String fqcn = resolveBridgeClassName(craftBukkitPackage);
        try {
            return (LegacyNbtBridge) Class.forName(fqcn).newInstance();
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot load isolated Legacy NBT bridge " + fqcn, exception);
        }
    }

    public static String resolveBridgeClassName(String craftBukkitPackage) {
        if (craftBukkitPackage.endsWith(".v1_8_R3")) {
            return "cl.dynasty.nexusbeacon.platform.legacy.bridge.v1_8_R3.LegacyNbtBridgeV1_8_R3";
        } else if (craftBukkitPackage.endsWith(".v1_12_R1")) {
            return "cl.dynasty.nexusbeacon.platform.legacy.bridge.v1_12_R1.LegacyNbtBridgeV1_12_R1";
        }
        throw new IllegalArgumentException("Unsupported Legacy CraftBukkit package: " + craftBukkitPackage);
    }
}
