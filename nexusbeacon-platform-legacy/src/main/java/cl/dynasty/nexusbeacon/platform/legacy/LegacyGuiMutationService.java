package cl.dynasty.nexusbeacon.platform.legacy;

import java.util.List;
import java.util.UUID;

/** Validates current state, persists it, then exposes the published replacement to the runtime. */
public final class LegacyGuiMutationService {
    public interface RuntimeReadiness { boolean isReady(); }

    private final LegacyApplicationState state;
    private final RuntimeReadiness runtime;
    private final List<LegacyBeamStylePlan> beamStyles;

    public LegacyGuiMutationService(LegacyApplicationState state, RuntimeReadiness runtime,
            List<LegacyBeamStylePlan> beamStyles) {
        if (state == null || runtime == null || beamStyles == null) throw new NullPointerException();
        this.state = state;
        this.runtime = runtime;
        this.beamStyles = beamStyles;
    }

    public LegacyGuiMutationResult toggleEffect(LegacyGuiSession session, UUID actor,
            boolean administrator, LegacyEffectDefinition definition) {
        LegacyBeaconState current = current(session);
        LegacyGuiMutationResult precondition = validate(current, session, actor, administrator);
        if (precondition != null) return precondition;
        if (definition == null || !definition.isSupported()) return LegacyGuiMutationResult.UNSUPPORTED;
        if (!current.getEffectLevels().containsKey(definition.getId())) {
            return LegacyGuiMutationResult.NOT_ACQUIRED;
        }
        if (!runtime.isReady()) return LegacyGuiMutationResult.RUNTIME_UNAVAILABLE;
        LegacyBeaconState replacement = current.withActiveEffect(definition.getId(),
                !current.getActiveEffects().contains(definition.getId()));
        return state.update(replacement) ? LegacyGuiMutationResult.COMMITTED
                : LegacyGuiMutationResult.MISSING_BEACON;
    }

    public LegacyGuiMutationResult selectBeamStyle(LegacyGuiSession session, UUID actor,
            boolean administrator, String styleId) {
        LegacyBeaconState current = current(session);
        LegacyGuiMutationResult precondition = validate(current, session, actor, administrator);
        if (precondition != null) return precondition;
        if (!knownStyle(styleId)) return LegacyGuiMutationResult.UNSUPPORTED;
        LegacyBeaconState replacement = current.withBeamStyle(styleId);
        if (replacement.equals(current)) return LegacyGuiMutationResult.COMMITTED;
        return state.update(replacement) ? LegacyGuiMutationResult.COMMITTED
                : LegacyGuiMutationResult.MISSING_BEACON;
    }

    public LegacyBeaconState currentAuthorized(LegacyGuiSession session, UUID actor, boolean administrator) {
        LegacyBeaconState current = current(session);
        return validate(current, session, actor, administrator) == null ? current : null;
    }

    private LegacyBeaconState current(LegacyGuiSession session) {
        return session == null ? null : state.findByUniqueId(session.getBeaconId());
    }

    private static LegacyGuiMutationResult validate(LegacyBeaconState current, LegacyGuiSession session,
            UUID actor, boolean administrator) {
        if (current == null) return LegacyGuiMutationResult.MISSING_BEACON;
        if (!current.getLocation().equals(session.getLocation())) return LegacyGuiMutationResult.STALE_LOCATION;
        if (!administrator && !actor.equals(current.getOwner())
                && !current.getTrustedPlayers().contains(actor)) {
            return LegacyGuiMutationResult.UNAUTHORIZED;
        }
        return null;
    }

    private boolean knownStyle(String id) {
        if (id == null) return false;
        for (LegacyBeamStylePlan style : beamStyles) if (style.getId().equalsIgnoreCase(id)) return true;
        return false;
    }
}
