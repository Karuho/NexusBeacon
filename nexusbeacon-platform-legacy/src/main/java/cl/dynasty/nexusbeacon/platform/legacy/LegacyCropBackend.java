package cl.dynasty.nexusbeacon.platform.legacy;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.block.Block;

import cl.dynasty.nexusbeacon.platform.api.MaterialContext;

/** Data-value adapter for crops which existed before Bukkit BlockData/Ageable. */
public final class LegacyCropBackend {
    public enum GrowthResult { GROWN, MATURE, NOT_A_SUPPORTED_CROP }

    private final Map<Material, CropRule> rules;

    public LegacyCropBackend(LegacyMaterialResolver materials) {
        if (materials == null) throw new NullPointerException("materials");
        Map<Material, CropRule> detected = new LinkedHashMap<Material, CropRule>();
        add(detected, materials, "WHEAT", new LinearRule(7));
        add(detected, materials, "CARROTS", new LinearRule(7));
        add(detected, materials, "POTATOES", new LinearRule(7));
        add(detected, materials, "NETHER_WART", new LinearRule(3));
        add(detected, materials, "PUMPKIN_STEM", new LinearRule(7));
        add(detected, materials, "MELON_STEM", new LinearRule(7));
        add(detected, materials, "COCOA", new CocoaRule());
        add(detected, materials, "BEETROOTS", new LinearRule(3));
        rules = Collections.unmodifiableMap(detected);
    }

    public GrowthResult growOneStage(Block block) {
        if (block == null) throw new NullPointerException("block");
        CropRule rule = rules.get(block.getType());
        if (rule == null) return GrowthResult.NOT_A_SUPPORTED_CROP;
        byte current = block.getData();
        int next = rule.next(current & 0xff);
        if (next < 0) return GrowthResult.MATURE;
        block.setData((byte) next, true);
        return GrowthResult.GROWN;
    }

    public boolean supports(Material material) { return rules.containsKey(material); }
    public boolean isImmature(Block block) {
        if (block == null) throw new NullPointerException("block");
        CropRule rule = rules.get(block.getType());
        return rule != null && rule.next(block.getData() & 0xff) >= 0;
    }
    public int supportedMaterialCount() { return rules.size(); }

    private static void add(Map<Material, CropRule> rules, LegacyMaterialResolver resolver,
            String identifier, CropRule rule) {
        LegacyMaterialResolution resolution = resolver.resolveLegacyMaterial(identifier, MaterialContext.BLOCK_MATCH);
        if (resolution.getResolution().isResolved()) {
            rules.put(resolution.getResolution().getMaterial().get(), rule);
        }
    }

    private interface CropRule { int next(int data); }

    private static final class LinearRule implements CropRule {
        private final int maximum;
        private LinearRule(int maximum) { this.maximum = maximum; }
        @Override public int next(int data) { return data >= maximum ? -1 : data + 1; }
    }

    private static final class CocoaRule implements CropRule {
        @Override public int next(int data) {
            int age = (data >> 2) & 0x3;
            return age >= 2 ? -1 : (data & 0x3) | ((age + 1) << 2);
        }
    }
}
