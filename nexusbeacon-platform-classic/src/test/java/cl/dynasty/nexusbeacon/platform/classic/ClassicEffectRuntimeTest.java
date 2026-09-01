package cl.dynasty.nexusbeacon.platform.classic;
import static org.junit.jupiter.api.Assertions.*;import java.util.Random;import org.junit.jupiter.api.Test;
class ClassicEffectRuntimeTest {
 @Test void normalPotionLevelsUseBukkitZeroBasedAmplifiers(){assertEquals(0,ClassicEffectRuntime.potionAmplifier(1,1));assertEquals(2,ClassicEffectRuntime.potionAmplifier(3,1));}
 @Test void cropWorkLimitsFailClosedAndStayBounded(){assertEquals(0,ClassicEffectRuntime.boundedWork(-1));assertEquals(16,ClassicEffectRuntime.boundedWork(16));assertEquals(2500,ClassicEffectRuntime.boundedWork(2500));}
 @Test void spawnerBaseComesFromMinMaxNotRemainingDelay(){Random fixed=new Random(){public int nextInt(int bound){return bound-1;}};assertEquals(799,ClassicEffectRuntime.randomizedBaseDelay(200,800,fixed));}
 @Test void spawnerPercentageAndLevelCooldownAreApplied(){assertEquals(720,ClassicEffectRuntime.boostedSpawnerDelay(800,10));assertEquals(400,ClassicEffectRuntime.boostedSpawnerDelay(800,50));assertEquals(8000L,ClassicEffectRuntime.cooldownMillis(160));assertEquals(4000L,ClassicEffectRuntime.cooldownMillis(80));}
 @Test void spawnerMetaConflictIsOptionalAndFailClosed(){assertTrue(ClassicEffectRuntime.spawnerConflict(true,true));assertFalse(ClassicEffectRuntime.spawnerConflict(false,true));assertFalse(ClassicEffectRuntime.spawnerConflict(true,false));}
 @Test void worldBoundsAcceptNegativeYOnlyWhenRuntimeMinimumAllowsIt(){assertEquals(0,ClassicWorldHeightCompatibility.minimumHeight(new OldWorld()));assertEquals(-64,ClassicWorldHeightCompatibility.minimumHeight(new NegativeWorld()));assertTrue(ClassicWorldHeightCompatibility.validY(-20,-64,320));assertTrue(ClassicWorldHeightCompatibility.validY(-64,-64,320));assertFalse(ClassicWorldHeightCompatibility.validY(-65,-64,320));assertTrue(ClassicWorldHeightCompatibility.validY(0,-64,320));assertFalse(ClassicWorldHeightCompatibility.validY(320,-64,320));assertFalse(ClassicWorldHeightCompatibility.validY(-20,0,256));}
 @Test void cropAndSpawnerAcceptValidNegativeYAndPreserveOldWorldBoundary(){assertTrue(ClassicEffectRuntime.cropYValid(-20,-64,320));assertTrue(ClassicEffectRuntime.spawnerYValid(-20,-64,320));assertFalse(ClassicEffectRuntime.cropYValid(-20,0,256));assertFalse(ClassicEffectRuntime.spawnerYValid(-20,0,256));}
 public static final class OldWorld{}
 public static final class NegativeWorld{public int getMinHeight(){return -64;}}
}
