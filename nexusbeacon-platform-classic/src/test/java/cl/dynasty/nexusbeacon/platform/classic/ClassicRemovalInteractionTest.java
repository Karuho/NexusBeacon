package cl.dynasty.nexusbeacon.platform.classic;
import static org.junit.jupiter.api.Assertions.*;import org.bukkit.GameMode;import org.bukkit.event.block.Action;import org.junit.jupiter.api.Test;
class ClassicRemovalInteractionTest {
 @Test void onlyRightClickBlockUsesManagedGuiPath(){assertTrue(ClassicCoreListener.opensGui(Action.RIGHT_CLICK_BLOCK));assertFalse(ClassicCoreListener.opensGui(Action.LEFT_CLICK_BLOCK));assertFalse(ClassicCoreListener.opensGui(Action.LEFT_CLICK_AIR));assertFalse(ClassicCoreListener.opensGui(Action.RIGHT_CLICK_AIR));assertFalse(ClassicCoreListener.opensGui(Action.PHYSICAL));}
 @Test void survivalAndCreativeBothReturnOnePortableItemPolicy(){assertTrue(ClassicCoreListener.returnsPortable(GameMode.SURVIVAL,true));assertTrue(ClassicCoreListener.returnsPortable(GameMode.CREATIVE,true));assertTrue(ClassicCoreListener.returnsPortable(GameMode.CREATIVE,false));}
}
