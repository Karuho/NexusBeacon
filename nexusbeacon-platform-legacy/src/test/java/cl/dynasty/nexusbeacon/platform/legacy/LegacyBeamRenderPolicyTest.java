package cl.dynasty.nexusbeacon.platform.legacy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.junit.jupiter.api.Test;

class LegacyBeamRenderPolicyTest {
    private static final LegacyBeaconLocation BEACON = new LegacyBeaconLocation("world", 0, 64, 0);
    private static final Set<Material> POWER = EnumSet.of(Material.IRON_BLOCK, Material.GOLD_BLOCK,
            Material.DIAMOND_BLOCK, Material.EMERALD_BLOCK, Material.REDSTONE_BLOCK, Material.LAPIS_BLOCK);

    @Test void productionRegressionTransitionsFromVanillaToConfiguredCustomRendering() {
        Blocks blocks = new Blocks(Material.IRON_BLOCK);
        LegacyBeamRenderPolicy policy = policy("AUTO");
        assertFalse(policy.shouldRender(blocks.world(), BEACON));

        blocks.set(3, 61, -2, Material.REDSTONE_BLOCK);
        assertTrue(policy.shouldRender(blocks.world(), BEACON));
    }

    @Test void allVanillaAndMixedVanillaBasesSuppressCustomBeam() {
        LegacyBeamRenderPolicy policy = policy("AUTO");
        assertFalse(policy.shouldRender(new Blocks(Material.IRON_BLOCK).world(), BEACON));

        Blocks two = new Blocks(Material.IRON_BLOCK);
        two.set(-1, 63, 1, Material.GOLD_BLOCK);
        assertFalse(policy.shouldRender(two.world(), BEACON));

        Blocks all = new Blocks(Material.IRON_BLOCK);
        all.set(-1, 63, 1, Material.GOLD_BLOCK);
        all.set(2, 62, 0, Material.DIAMOND_BLOCK);
        all.set(-3, 61, 3, Material.EMERALD_BLOCK);
        assertFalse(policy.shouldRender(all.world(), BEACON));
    }

    @Test void anyConfiguredCustomMaterialAcrossAllLayersEnablesCustomBeam() {
        LegacyBeamRenderPolicy policy = policy("CUSTOM_ONLY");
        Blocks mixed = new Blocks(Material.IRON_BLOCK);
        mixed.set(4, 60, 4, Material.REDSTONE_BLOCK);
        assertTrue(policy.shouldRender(mixed.world(), BEACON));

        mixed.set(-4, 60, -4, Material.LAPIS_BLOCK);
        assertTrue(policy.shouldRender(mixed.world(), BEACON));
    }

    @Test void unconfiguredMaterialDoesNotBecomeCustomPowerBlock() {
        Blocks blocks = new Blocks(Material.IRON_BLOCK);
        blocks.set(0, 63, 0, Material.COAL_BLOCK);
        assertFalse(policy("AUTO").shouldRender(blocks.world(), BEACON));
    }

    @Test void renderModesPreserveKnownGoodSemantics() {
        Blocks vanilla = new Blocks(Material.IRON_BLOCK);
        Blocks custom = new Blocks(Material.IRON_BLOCK);
        custom.set(0, 63, 0, Material.REDSTONE_BLOCK);

        assertTrue(policy("ALWAYS").shouldRender(vanilla.world(), BEACON));
        assertFalse(policy("AUTO").shouldRender(vanilla.world(), BEACON));
        assertTrue(policy("AUTO").shouldRender(custom.world(), BEACON));
        assertFalse(policy("CUSTOM_ONLY").shouldRender(vanilla.world(), BEACON));
        assertTrue(policy("CUSTOM_ONLY").shouldRender(custom.world(), BEACON));
    }

    @Test void missingWorldOrLocationCannotEnableCustomRendering() {
        assertFalse(policy("AUTO").shouldRender(null, BEACON));
        assertFalse(policy("AUTO").shouldRender(new Blocks(Material.REDSTONE_BLOCK).world(), null));
    }

    private static LegacyBeamRenderPolicy policy(String mode) {
        return new LegacyBeamRenderPolicy(mode, 4, POWER);
    }

    private static final class Blocks {
        private final Material fallback;
        private final Map<String, Material> values = new HashMap<String, Material>();

        private Blocks(Material fallback) { this.fallback = fallback; }

        private void set(int x, int y, int z, Material material) { values.put(key(x, y, z), material); }

        private World world() {
            return (World) Proxy.newProxyInstance(World.class.getClassLoader(), new Class<?>[] { World.class },
                    (proxy, method, args) -> {
                        if (!"getBlockAt".equals(method.getName()) || args.length != 3) return null;
                        Material material = values.get(key((Integer) args[0], (Integer) args[1], (Integer) args[2]));
                        return block(material == null ? fallback : material);
                    });
        }

        private static Block block(Material material) {
            return (Block) Proxy.newProxyInstance(Block.class.getClassLoader(), new Class<?>[] { Block.class },
                    (proxy, method, args) -> "getType".equals(method.getName()) ? material : null);
        }

        private static String key(int x, int y, int z) { return x + ";" + y + ";" + z; }
    }
}
