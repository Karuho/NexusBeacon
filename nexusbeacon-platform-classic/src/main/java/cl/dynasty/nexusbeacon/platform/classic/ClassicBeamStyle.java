package cl.dynasty.nexusbeacon.platform.classic;
import org.bukkit.Color;import org.bukkit.Particle;
public final class ClassicBeamStyle {private final String id;private final Particle particle;private final Color color;private final float size;private final boolean degraded;
 public ClassicBeamStyle(String id,Particle particle,Color color,float size,boolean degraded){if(id==null||particle==null||size<=0)throw new IllegalArgumentException("Invalid beam style");this.id=id;this.particle=particle;this.color=color;this.size=size;this.degraded=degraded;}
 public String getId(){return id;}public Particle getParticle(){return particle;}public Color getColor(){return color;}public float getSize(){return size;}public boolean isDegraded(){return degraded;}}
