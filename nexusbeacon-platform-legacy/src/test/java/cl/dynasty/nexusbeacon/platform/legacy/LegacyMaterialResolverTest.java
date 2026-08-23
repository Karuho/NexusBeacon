package cl.dynasty.nexusbeacon.platform.legacy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import cl.dynasty.nexusbeacon.platform.api.MaterialContext;
import cl.dynasty.nexusbeacon.platform.api.MaterialResolutionStatus;

class LegacyMaterialResolverTest {
    private final LegacyMaterialResolver resolver = new LegacyMaterialResolver();

    @Test void resolvesSharedMaterialExactly() {
        LegacyMaterialResolution result = resolver.resolveLegacyMaterial("BEACON", MaterialContext.REQUIRED_ITEM);
        assertTrue(result.getResolution().isResolved());
        assertEquals(Material.BEACON, result.getResolution().getMaterial().get());
        assertEquals(LegacyMaterialMappingKind.EXACT, result.getMappingKind());
    }

    @Test void normalizesNamespaceWhitespaceAndCase() {
        assertTrue(resolver.resolveMaterial(" minecraft:beacon ", MaterialContext.REQUIRED_ITEM).isResolved());
    }

    @Test void mapsCobwebToItsLegacyName() {
        LegacyMaterialResolution result = resolver.resolveLegacyMaterial("cobweb", MaterialContext.EFFECT_ICON);
        assertEquals(Material.WEB, result.getResolution().getMaterial().get());
        assertEquals(LegacyMaterialMappingKind.COMPATIBLE_ALIAS, result.getMappingKind());
    }

    @Test void preservesBlackPaneDataValue() {
        LegacyMaterialResolution result = resolver.resolveLegacyMaterial("black-stained-glass-pane", MaterialContext.GUI_ICON);
        assertEquals(Material.STAINED_GLASS_PANE, result.getResolution().getMaterial().get());
        assertEquals(15, result.getData());
    }

    @Test void preservesConfiguredDyeDataValues() {
        assertDye("LIGHT_BLUE_DYE", 12);
        assertDye("RED_DYE", 1);
        assertDye("LIME_DYE", 10);
        assertDye("PURPLE_DYE", 5);
    }

    @Test void mapsSpawnerAndEnderEyeNames() {
        assertEquals(Material.MOB_SPAWNER, resolver.resolveMaterial("SPAWNER", MaterialContext.EFFECT_ICON).getMaterial().get());
        assertEquals(Material.EYE_OF_ENDER, resolver.resolveMaterial("ENDER_EYE", MaterialContext.EFFECT_ICON).getMaterial().get());
    }

    @Test void rejectsUnsupportedGameplayMaterialWithoutFallback() {
        LegacyMaterialResolution result = resolver.resolveLegacyMaterial("NETHERITE_SWORD", MaterialContext.REQUIRED_ITEM);
        assertEquals(MaterialResolutionStatus.UNSUPPORTED, result.getResolution().getStatus());
        assertFalse(result.getResolution().getMaterial().isPresent());
        assertFalse(result.getResolution().isFallbackUsed());
    }

    @Test void usesStoneOnlyForPermittedVisualFallback() {
        LegacyMaterialResolution result = resolver.resolveLegacyMaterial("REINFORCED_DEEPSLATE", MaterialContext.GUI_ICON);
        assertEquals(MaterialResolutionStatus.UNSUPPORTED, result.getResolution().getStatus());
        assertEquals(Material.STONE, result.getResolution().getMaterial().get());
        assertTrue(result.getResolution().isFallbackUsed());
        assertEquals(LegacyMaterialMappingKind.VISUAL_FALLBACK, result.getMappingKind());
    }

    @Test void effectIconsDoNotSilentlyFallback() {
        assertFalse(resolver.resolveMaterial("BLAST_FURNACE", MaterialContext.EFFECT_ICON).getMaterial().isPresent());
    }

    @Test void usesRequestedVersionAwareGuiIconsAndExactWheatItem() {
        assertEquals(Material.WATER_BUCKET,
                resolver.resolveMaterial("TURTLE_HELMET", MaterialContext.GUI_ICON).getMaterial().get());
        assertEquals(Material.DIAMOND_CHESTPLATE,
                resolver.resolveMaterial("SHIELD", MaterialContext.GUI_ICON).getMaterial().get());
        assertEquals(Material.CACTUS,
                resolver.resolveMaterial("NETHERITE_SWORD", MaterialContext.GUI_ICON).getMaterial().get());
        assertEquals(Material.WHEAT,
                resolver.resolveMaterial("WHEAT", MaterialContext.GUI_ICON).getMaterial().get());
        assertEquals(Material.CROPS,
                resolver.resolveMaterial("WHEAT", MaterialContext.BLOCK_MATCH).getMaterial().get());
    }

    @Test void distinguishesMalformedIdentifier() {
        assertEquals(MaterialResolutionStatus.INVALID_IDENTIFIER,
                resolver.resolveMaterial("bad/id", MaterialContext.GUI_ICON).getStatus());
        assertEquals(MaterialResolutionStatus.INVALID_IDENTIFIER,
                resolver.resolveMaterial(" ", MaterialContext.REQUIRED_ITEM).getStatus());
    }

    private void assertDye(String identifier, int data) {
        LegacyMaterialResolution result = resolver.resolveLegacyMaterial(identifier, MaterialContext.GUI_ICON);
        assertEquals(Material.INK_SACK, result.getResolution().getMaterial().get());
        assertEquals(data, result.getData());
    }
}
