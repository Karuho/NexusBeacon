package cl.dynasty.nexusbeacon.platform.classic;
import java.util.Locale;import java.util.Map;
/** Productive mutations exposed by the Phase 30 Classic GUI. Effects intentionally remain deferred. */
public final class ClassicGuiMutationService {
 public enum Result {COMMITTED,MISSING_BEACON,UNSUPPORTED,EXECUTOR_UNAVAILABLE}
 private final ClassicApplicationState state;private final Map<String,ClassicBeamStyle> styles;
 public ClassicGuiMutationService(ClassicApplicationState state,Map<String,ClassicBeamStyle> styles){this.state=state;this.styles=styles;}
 public Result toggleRange(ClassicBeaconLocation location){ClassicBeaconRecord r=state.find(location);if(r==null)return Result.MISSING_BEACON;return state.update(r.withVisuals(r.getBeamStyle(),!r.isRangeParticles(),r.getRangeParticleType()))?Result.COMMITTED:Result.MISSING_BEACON;}
 public Result selectBeam(ClassicBeaconLocation location,String id){ClassicBeaconRecord r=state.find(location);if(r==null)return Result.MISSING_BEACON;if(id==null||!styles.containsKey(id.toLowerCase(Locale.ROOT)))return Result.UNSUPPORTED;return state.update(r.withVisuals(id,r.isRangeParticles(),r.getRangeParticleType()))?Result.COMMITTED:Result.MISSING_BEACON;}
 public Result activateEffect(ClassicBeaconLocation location,String id){return state.find(location)==null?Result.MISSING_BEACON:Result.EXECUTOR_UNAVAILABLE;}
}
