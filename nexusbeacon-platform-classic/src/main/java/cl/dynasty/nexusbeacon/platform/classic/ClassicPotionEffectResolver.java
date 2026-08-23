package cl.dynasty.nexusbeacon.platform.classic;

import java.util.Locale;
import java.util.function.Function;
import org.bukkit.potion.PotionEffectType;
import cl.dynasty.nexusbeacon.platform.api.PotionEffectResolution;
import cl.dynasty.nexusbeacon.platform.api.PotionEffectResolver;

public final class ClassicPotionEffectResolver implements PotionEffectResolver {
    private final Function<String, PotionEffectType> lookup;
    public ClassicPotionEffectResolver() { this(new Function<String, PotionEffectType>() { public PotionEffectType apply(String name) { return PotionEffectType.getByName(name); } }); }
    ClassicPotionEffectResolver(Function<String, PotionEffectType> lookup) { if (lookup == null) throw new NullPointerException("lookup"); this.lookup = lookup; }
    @Override public PotionEffectResolution resolvePotionEffect(String identifier) {
        if (identifier == null || identifier.trim().isEmpty()) {
            return PotionEffectResolution.failed(identifier, PotionEffectResolution.Status.INVALID_IDENTIFIER);
        }
        PotionEffectType type = lookup.apply(identifier.trim().toUpperCase(Locale.ROOT));
        return type == null ? PotionEffectResolution.failed(identifier, PotionEffectResolution.Status.UNSUPPORTED)
                : PotionEffectResolution.resolved(identifier, type);
    }
}
