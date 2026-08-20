package cl.dynasty.nexusbeacon.platform.legacy;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import cl.dynasty.nexusbeacon.platform.api.MaterialContext;

/** Exact level-first/global-fallback Modern payment option lookup with fail-closed validation. */
final class LegacyPaymentOptionResolver {
    private final FileConfiguration effects;
    private final LegacyMaterialResolver materials;

    LegacyPaymentOptionResolver(FileConfiguration effects, LegacyMaterialResolver materials) {
        if (effects == null || materials == null) throw new NullPointerException();
        this.effects = effects;
        this.materials = materials;
    }

    boolean isLevelEnabled(String effectId, int level) {
        String path = "effects." + effectId + ".levels." + level;
        ConfigurationSection section = effects.getConfigurationSection(path);
        return section == null || section.getBoolean("enabled", true);
    }

    LegacyPaymentOption resolve(String effectId, String action, String optionKey, int level) {
        String suffix = ".costs." + action + ".options." + optionKey;
        ConfigurationSection section = effects.getConfigurationSection(
                "effects." + effectId + ".levels." + level + suffix);
        if (section == null) section = effects.getConfigurationSection("effects." + effectId + suffix);
        if (section == null) return null;
        String rawType = section.getString("type");
        if (rawType == null) return null;
        final LegacyPaymentOption.Type type;
        try { type = LegacyPaymentOption.Type.valueOf(rawType.toUpperCase(java.util.Locale.ROOT)); }
        catch (IllegalArgumentException invalid) { return null; }
        if (type == LegacyPaymentOption.Type.NONE) return new LegacyPaymentOption(type, 0, null);
        Integer amount = amount(section, level);
        if (amount == null || amount.intValue() <= 0) return null;
        Material material = null;
        if (type == LegacyPaymentOption.Type.ITEM) {
            String name = section.getString("material");
            if (name == null) return null;
            LegacyMaterialResolution resolution = materials.resolveLegacyMaterial(name, MaterialContext.PAYMENT);
            if (!resolution.getResolution().isResolved()) return null;
            material = resolution.getResolution().getMaterial().get();
        }
        return new LegacyPaymentOption(type, amount.intValue(), material);
    }

    private static Integer amount(ConfigurationSection section, int level) {
        Object exact = section.get("amount");
        if (exact instanceof Integer) return (Integer) exact;
        Object perLevel = section.get("amount-per-level");
        if (!(perLevel instanceof Integer)) return null;
        long calculated = (long) ((Integer) perLevel).intValue() * level;
        return calculated > Integer.MAX_VALUE || calculated < Integer.MIN_VALUE
                ? null : Integer.valueOf((int) calculated);
    }
}
