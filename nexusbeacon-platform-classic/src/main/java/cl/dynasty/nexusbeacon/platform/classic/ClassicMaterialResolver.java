package cl.dynasty.nexusbeacon.platform.classic;

import java.util.Locale;
import org.bukkit.Material;
import cl.dynasty.nexusbeacon.platform.api.MaterialContext;
import cl.dynasty.nexusbeacon.platform.api.MaterialResolution;
import cl.dynasty.nexusbeacon.platform.api.MaterialResolutionStatus;
import cl.dynasty.nexusbeacon.platform.api.MaterialResolver;

public final class ClassicMaterialResolver implements MaterialResolver {
    @Override public MaterialResolution resolveMaterial(String identifier, MaterialContext context) {
        if (context == null) throw new NullPointerException("context");
        if (identifier == null || identifier.trim().isEmpty()) {
            return MaterialResolution.failed(identifier, context, MaterialResolutionStatus.INVALID_IDENTIFIER, null);
        }
        Material material = Material.matchMaterial(identifier.trim().toUpperCase(Locale.ROOT));
        return material == null
                ? MaterialResolution.failed(identifier, context, MaterialResolutionStatus.UNSUPPORTED, null)
                : MaterialResolution.resolved(identifier, context, material);
    }
}
