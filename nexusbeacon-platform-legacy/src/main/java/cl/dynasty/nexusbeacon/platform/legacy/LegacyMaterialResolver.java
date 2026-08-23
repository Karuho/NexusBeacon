package cl.dynasty.nexusbeacon.platform.legacy;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.bukkit.Material;

import cl.dynasty.nexusbeacon.platform.api.MaterialContext;
import cl.dynasty.nexusbeacon.platform.api.MaterialResolution;
import cl.dynasty.nexusbeacon.platform.api.MaterialResolutionStatus;
import cl.dynasty.nexusbeacon.platform.api.MaterialResolver;

public final class LegacyMaterialResolver implements MaterialResolver {
    private static final Map<String, LegacyMaterial> ALIASES = aliases();
    private static final Map<String, LegacyMaterial> GUI_ALIASES = guiAliases();

    @Override
    public MaterialResolution resolveMaterial(String identifier, MaterialContext context) {
        return resolveLegacyMaterial(identifier, context).getResolution();
    }

    public LegacyMaterialResolution resolveLegacyMaterial(String identifier, MaterialContext context) {
        if (context == null) throw new NullPointerException("context");
        if (identifier == null || identifier.trim().isEmpty()) {
            return failed(identifier, context, MaterialResolutionStatus.INVALID_IDENTIFIER);
        }

        String normalized = normalize(identifier);
        if (!normalized.matches("[A-Z0-9_]+")) {
            return failed(identifier, context, MaterialResolutionStatus.INVALID_IDENTIFIER);
        }

        Material exact = Material.getMaterial(normalized);
        if (context == MaterialContext.GUI_ICON && exact != null) {
            return new LegacyMaterialResolution(MaterialResolution.resolved(identifier, context, exact),
                    (short) 0, LegacyMaterialMappingKind.EXACT);
        }

        LegacyMaterial alias = context == MaterialContext.GUI_ICON ? GUI_ALIASES.get(normalized) : null;
        if (alias == null) alias = ALIASES.get(normalized);
        if (alias != null) {
            Material material = Material.getMaterial(alias.name);
            if (material != null) {
                return new LegacyMaterialResolution(MaterialResolution.resolved(identifier, context, material),
                        alias.data, LegacyMaterialMappingKind.COMPATIBLE_ALIAS);
            }
        }

        exact = Material.getMaterial(normalized);
        if (exact != null) {
            return new LegacyMaterialResolution(MaterialResolution.resolved(identifier, context, exact),
                    (short) 0, LegacyMaterialMappingKind.EXACT);
        }
        return failed(identifier, context, MaterialResolutionStatus.UNSUPPORTED);
    }

    private LegacyMaterialResolution failed(String identifier, MaterialContext context,
            MaterialResolutionStatus status) {
        if (context.isVisualFallbackAllowed()) {
            return new LegacyMaterialResolution(MaterialResolution.failed(identifier, context, status, Material.STONE),
                    (short) 0, LegacyMaterialMappingKind.VISUAL_FALLBACK);
        }
        return new LegacyMaterialResolution(MaterialResolution.failed(identifier, context, status, null),
                (short) 0, LegacyMaterialMappingKind.UNRESOLVED);
    }

    private static String normalize(String identifier) {
        return identifier.trim().toUpperCase(Locale.ROOT)
                .replace("MINECRAFT:", "")
                .replace(" ", "_")
                .replace("-", "_");
    }

    private static Map<String, LegacyMaterial> aliases() {
        Map<String, LegacyMaterial> aliases = new HashMap<String, LegacyMaterial>();
        aliases.put("BLACK_STAINED_GLASS_PANE", new LegacyMaterial("STAINED_GLASS_PANE", 15));
        aliases.put("LIGHT_BLUE_DYE", new LegacyMaterial("INK_SACK", 12));
        aliases.put("RED_DYE", new LegacyMaterial("INK_SACK", 1));
        aliases.put("LIME_DYE", new LegacyMaterial("INK_SACK", 10));
        aliases.put("PURPLE_DYE", new LegacyMaterial("INK_SACK", 5));
        aliases.put("COBWEB", new LegacyMaterial("WEB", 0));
        aliases.put("SPAWNER", new LegacyMaterial("MOB_SPAWNER", 0));
        aliases.put("ENDER_EYE", new LegacyMaterial("EYE_OF_ENDER", 0));
        aliases.put("WHEAT", new LegacyMaterial("CROPS", 0));
        aliases.put("CARROTS", new LegacyMaterial("CARROT", 0));
        aliases.put("POTATOES", new LegacyMaterial("POTATO", 0));
        aliases.put("NETHER_WART", new LegacyMaterial("NETHER_WARTS", 0));
        aliases.put("BEETROOTS", new LegacyMaterial("BEETROOT_BLOCK", 0));
        return Collections.unmodifiableMap(aliases);
    }

    private static Map<String, LegacyMaterial> guiAliases() {
        Map<String, LegacyMaterial> aliases = new HashMap<String, LegacyMaterial>();
        aliases.put("TURTLE_HELMET", new LegacyMaterial("WATER_BUCKET", 0));
        aliases.put("SHIELD", new LegacyMaterial("DIAMOND_CHESTPLATE", 0));
        aliases.put("NETHERITE_SWORD", new LegacyMaterial("CACTUS", 0));
        return Collections.unmodifiableMap(aliases);
    }

    private static final class LegacyMaterial {
        private final String name;
        private final short data;

        private LegacyMaterial(String name, int data) {
            this.name = name;
            this.data = (short) data;
        }
    }
}
