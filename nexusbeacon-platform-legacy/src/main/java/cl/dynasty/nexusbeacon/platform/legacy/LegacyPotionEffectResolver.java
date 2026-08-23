package cl.dynasty.nexusbeacon.platform.legacy;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

import org.bukkit.potion.PotionEffectType;

import cl.dynasty.nexusbeacon.platform.api.PotionEffectResolution;
import cl.dynasty.nexusbeacon.platform.api.PotionEffectResolver;

public final class LegacyPotionEffectResolver implements PotionEffectResolver {
    private static final Map<String, String> ALIASES = aliases();
    private final Function<String, PotionEffectType> lookup;

    public LegacyPotionEffectResolver() {
        this(new Function<String, PotionEffectType>() {
            @Override public PotionEffectType apply(String name) { return PotionEffectType.getByName(name); }
        });
    }

    LegacyPotionEffectResolver(Function<String, PotionEffectType> lookup) {
        if (lookup == null) throw new NullPointerException("lookup");
        this.lookup = lookup;
    }

    @Override
    public PotionEffectResolution resolvePotionEffect(String identifier) {
        if (identifier == null || identifier.trim().isEmpty()) {
            return PotionEffectResolution.failed(identifier, PotionEffectResolution.Status.INVALID_IDENTIFIER);
        }
        String normalized = normalize(identifier);
        if (!normalized.matches("[A-Z0-9_]+")) {
            return PotionEffectResolution.failed(identifier, PotionEffectResolution.Status.INVALID_IDENTIFIER);
        }
        String legacyName = ALIASES.containsKey(normalized) ? ALIASES.get(normalized) : normalized;
        PotionEffectType resolved = lookup.apply(legacyName);
        return resolved == null
                ? PotionEffectResolution.failed(identifier, PotionEffectResolution.Status.UNSUPPORTED)
                : PotionEffectResolution.resolved(identifier, resolved);
    }

    private static String normalize(String identifier) {
        return identifier.trim().toUpperCase(Locale.ROOT)
                .replace("MINECRAFT:", "")
                .replace(" ", "_")
                .replace("-", "_");
    }

    private static Map<String, String> aliases() {
        Map<String, String> aliases = new HashMap<String, String>();
        aliases.put("STRENGTH", "INCREASE_DAMAGE");
        aliases.put("HASTE", "FAST_DIGGING");
        aliases.put("RESISTANCE", "DAMAGE_RESISTANCE");
        aliases.put("SLOWNESS", "SLOW");
        aliases.put("JUMP_BOOST", "JUMP");
        aliases.put("NAUSEA", "CONFUSION");
        aliases.put("INSTANT_HEALTH", "HEAL");
        aliases.put("INSTANT_DAMAGE", "HARM");
        return Collections.unmodifiableMap(aliases);
    }
}
