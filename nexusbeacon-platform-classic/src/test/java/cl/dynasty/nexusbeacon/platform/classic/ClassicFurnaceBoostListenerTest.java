package cl.dynasty.nexusbeacon.platform.classic;
import static org.junit.jupiter.api.Assertions.*;import java.lang.reflect.Method;import org.bukkit.event.EventHandler;import org.junit.jupiter.api.Test;
class ClassicFurnaceBoostListenerTest {
 @Test void configuredValuesArePercentagesForBurnAndCook(){assertEquals(184,ClassicFurnaceBoostListener.reducedBurnTime(200,8));assertEquals(152,ClassicFurnaceBoostListener.reducedBurnTime(200,24));assertEquals(16,ClassicFurnaceBoostListener.advancedCookTime(0,200,8));assertEquals(48,ClassicFurnaceBoostListener.advancedCookTime(0,200,24));}
 @Test void cookAdvanceCapsWithoutCreatingOutput(){assertEquals(200,ClassicFurnaceBoostListener.advancedCookTime(195,200,24));assertEquals(1,ClassicFurnaceBoostListener.reducedBurnTime(1,99));}
 @Test void bestOverlappingBoostWinsOnceWithoutStacking(){ClassicFurnaceBoostListener.Boost low=new ClassicFurnaceBoostListener.Boost(8,8),high=new ClassicFurnaceBoostListener.Boost(24,24);assertSame(high,ClassicFurnaceBoostListener.best(low,high));assertEquals(48,ClassicFurnaceBoostListener.advancedCookTime(0,200,ClassicFurnaceBoostListener.best(low,high).cookPercent));}
 @Test void inactiveNonAuthoritativeAndOutsideFurnacesAreIgnored(){assertFalse(ClassicFurnaceBoostListener.eligible(false,true,true));assertFalse(ClassicFurnaceBoostListener.eligible(true,false,true));assertFalse(ClassicFurnaceBoostListener.eligible(true,true,false));assertFalse(ClassicFurnaceBoostListener.insideHorizontal(49,0,0,0,48));assertTrue(ClassicFurnaceBoostListener.insideHorizontal(48,0,0,0,48));}
 @Test void cancelledFurnaceEventsAreIgnored()throws Exception{assertTrue(handler("burn").ignoreCancelled());assertTrue(handler("smelt").ignoreCancelled());}
 private static EventHandler handler(String name)throws Exception{for(Method method:ClassicFurnaceBoostListener.class.getDeclaredMethods())if(method.getName().equals(name))return method.getAnnotation(EventHandler.class);throw new AssertionError(name);}
}
