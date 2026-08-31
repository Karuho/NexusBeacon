package cl.dynasty.nexusbeacon.platform.classic;

import static org.junit.jupiter.api.Assertions.*;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffectType;
import org.junit.jupiter.api.Test;
import cl.dynasty.nexusbeacon.platform.api.MaterialContext;
import cl.dynasty.nexusbeacon.platform.api.PotionEffectResolution;

class ClassicResolversTest {
    @Test void resolvesKnownMaterialWithoutSubstitution() { assertEquals(Material.BEACON, new ClassicMaterialResolver().resolveMaterial("beacon", MaterialContext.REQUIRED_ITEM).getMaterial().get()); }
    @Test void unknownMaterialFailsClosed() { assertFalse(new ClassicMaterialResolver().resolveMaterial("future_block", MaterialContext.REQUIRED_ITEM).isResolved()); }
    @Test void resolvesKnownPotionMeaning() { assertTrue(new ClassicPotionEffectResolver(name -> PotionEffectType.SPEED).resolvePotionEffect("speed").isResolved()); }
    @Test void strengthUsesHistoricalIncreaseDamageAlias() { assertTrue(new ClassicPotionEffectResolver(name -> "INCREASE_DAMAGE".equals(name)?PotionEffectType.INCREASE_DAMAGE:null).resolvePotionEffect("strength").isResolved()); }
    @Test void unknownPotionFailsClosed() { assertEquals(PotionEffectResolution.Status.UNSUPPORTED, new ClassicPotionEffectResolver().resolvePotionEffect("future_effect").getStatus()); }
    @Test void resolvesKnownParticle() { assertTrue(new ClassicParticleResolver().resolve("flame").isResolved()); }
    @Test void unknownParticleDegradesExplicitly() { assertEquals(ClassicParticleResolution.Status.UNSUPPORTED, new ClassicParticleResolver().resolve("future_particle").getStatus()); }
}
