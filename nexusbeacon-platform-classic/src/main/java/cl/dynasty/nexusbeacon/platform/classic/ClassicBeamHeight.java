package cl.dynasty.nexusbeacon.platform.classic;
import org.bukkit.configuration.ConfigurationSection;
/** Backward-compatible custom beam height mode resolution. */
final class ClassicBeamHeight {
 static final String FIXED="FIXED";static final String WORLD_MAX="WORLD_MAX";
 private ClassicBeamHeight(){}
 static String mode(ConfigurationSection config){String configured=config.getString("visual-beam.height-mode");return WORLD_MAX.equalsIgnoreCase(configured)?WORLD_MAX:FIXED;}
 static int height(String mode,int fixedHeight,int beaconY,int worldMaximum){return WORLD_MAX.equals(mode)?Math.max(1,worldMaximum-(beaconY+1)):Math.max(1,fixedHeight);}
}
