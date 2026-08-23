package cl.dynasty.nexusbeacon.platform.legacy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.junit.jupiter.api.Test;

class LegacyCropBackendTest {
    @Test void growsEveryAvailableLinearLegacyCropExactlyOneDataStage() {
        LegacyCropBackend backend = new LegacyCropBackend(new LegacyMaterialResolver());
        String[] identifiers = { "CROPS", "CARROT", "POTATO", "NETHER_WARTS",
                "PUMPKIN_STEM", "MELON_STEM", "BEETROOT_BLOCK" };
        int tested = 0;
        for (String identifier : identifiers) {
            Material material = Material.getMaterial(identifier);
            if (material == null) continue;
            BlockRecorder block = new BlockRecorder(material, (byte) 1);
            assertTrue(backend.isImmature(block.proxy), identifier);
            assertEquals(LegacyCropBackend.GrowthResult.GROWN, backend.growOneStage(block.proxy), identifier);
            assertEquals(2, block.data & 0xff, identifier);
            assertTrue(block.physics, identifier);
            tested++;
        }
        assertEquals(Material.getMaterial("BEETROOT_BLOCK") == null ? 6 : 7, tested);
    }

    @Test void preservesCocoaFacingBitsWhileIncrementingOnlyItsAge() {
        LegacyCropBackend backend = new LegacyCropBackend(new LegacyMaterialResolver());
        BlockRecorder block = new BlockRecorder(Material.COCOA, (byte) 5); // age 1, facing bits 1

        assertEquals(LegacyCropBackend.GrowthResult.GROWN, backend.growOneStage(block.proxy));

        assertEquals(9, block.data & 0xff);
        assertEquals(1, (block.data & 0xff) & 0x3);
        assertFalse(backend.isImmature(block.proxy));
        assertEquals(LegacyCropBackend.GrowthResult.MATURE, backend.growOneStage(block.proxy));
    }

    @Test void rejectsUnsupportedBlocksWithoutMutation() {
        LegacyCropBackend backend = new LegacyCropBackend(new LegacyMaterialResolver());
        BlockRecorder block = new BlockRecorder(Material.STONE, (byte) 3);

        assertEquals(LegacyCropBackend.GrowthResult.NOT_A_SUPPORTED_CROP,
                backend.growOneStage(block.proxy));
        assertEquals(3, block.data & 0xff);
        assertFalse(block.mutated);
    }

    private static final class BlockRecorder implements InvocationHandler {
        private final Material material;
        private byte data;
        private boolean physics;
        private boolean mutated;
        private final Block proxy = (Block) Proxy.newProxyInstance(Block.class.getClassLoader(),
                new Class<?>[] { Block.class }, this);

        private BlockRecorder(Material material, byte data) {
            this.material = material;
            this.data = data;
        }

        @Override public Object invoke(Object proxy, Method method, Object[] args) {
            if ("getType".equals(method.getName())) return material;
            if ("getData".equals(method.getName())) return Byte.valueOf(data);
            if ("setData".equals(method.getName())) {
                data = ((Byte) args[0]).byteValue();
                physics = args.length > 1 && ((Boolean) args[1]).booleanValue();
                mutated = true;
                return null;
            }
            return defaultValue(method.getReturnType());
        }
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0F;
        if (type == double.class) return 0D;
        return '\0';
    }
}
