package cl.dynasty.nexusbeacon.platform.classic;
import java.util.Iterator;import java.util.Map;import java.util.Random;
/** Transient cycle observation that never postpones an active spawner countdown. */
final class ClassicSpawnerCountdown {
 static final long STALE_SCAN_AGE=300L;
 static final class State{private int previousDelay;private long lastSeenScan;private long nextEligibleMillis;private boolean observed;private boolean pendingCycle;}
 private ClassicSpawnerCountdown(){}
 static int observe(State state,int current,int minimum,int maximum,double percent,Random random,long scan,long now,long cooldownMillis){state.lastSeenScan=scan;if(!state.observed||current>state.previousDelay)state.pendingCycle=true;int result=current;if(state.pendingCycle&&now>=state.nextEligibleMillis){int proposed=ClassicEffectRuntime.boostedSpawnerDelay(ClassicEffectRuntime.randomizedBaseDelay(minimum,maximum,random),percent);result=Math.min(current,proposed);state.pendingCycle=false;state.nextEligibleMillis=now+Math.max(0L,cooldownMillis);}state.previousDelay=result;state.observed=true;return result;}
 static void clean(Map<String,State> states,long scan){Iterator<State> values=states.values().iterator();while(values.hasNext())if(scan-values.next().lastSeenScan>STALE_SCAN_AGE)values.remove();}
}
