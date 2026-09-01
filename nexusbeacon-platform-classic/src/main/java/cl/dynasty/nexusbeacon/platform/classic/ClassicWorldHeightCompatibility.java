package cl.dynasty.nexusbeacon.platform.classic;
import java.lang.reflect.Method;
/** Runtime-safe world height access across the complete Classic server generation. */
final class ClassicWorldHeightCompatibility {
 private ClassicWorldHeightCompatibility(){}
 static int minimumHeight(Object world){try{Method method=world.getClass().getMethod("getMinHeight");return ((Integer)method.invoke(world)).intValue();}catch(Exception oldApi){return 0;}}
 static boolean validY(int y,int minimum,int maximum){return y>=minimum&&y<maximum;}
}
