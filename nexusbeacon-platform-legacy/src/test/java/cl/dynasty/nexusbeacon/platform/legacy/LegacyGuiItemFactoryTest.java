package cl.dynasty.nexusbeacon.platform.legacy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.bukkit.Material;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cl.dynasty.nexusbeacon.platform.api.MaterialContext;

class LegacyGuiItemFactoryTest {
    private LegacyGuiItemFactory factory;

    @BeforeEach
    void setUp() {
        factory = new LegacyGuiItemFactory(new LegacyMaterialResolver(), new LegacyTextFormatter());
    }

    @Test
    void plansSharedGuiMaterialWithPresentation() {
        LegacyGuiItemPlan plan = factory.planItem("BEACON", MaterialContext.GUI_ICON, "&aBeacon",
                Arrays.asList("&7Line one", "plain"), null);
        assertEquals(Material.BEACON, plan.getMaterial());
        assertEquals(0, plan.getData());
        assertEquals("\u00a7aBeacon", plan.getDisplayName());
        assertEquals(Arrays.asList("\u00a77Line one", "plain"), plan.getLore());
        assertFalse(plan.isVisualFallback());
    }

    @Test
    void appliesLegacyDataValueFromExistingResolver() {
        LegacyGuiItemPlan plan = factory.planItem("BLACK_STAINED_GLASS_PANE", MaterialContext.GUI_ICON,
                " ", Collections.<String>emptyList(), null);
        assertEquals(Material.STAINED_GLASS_PANE, plan.getMaterial());
        assertEquals(15, plan.getData());
    }

    @Test
    void usesExplicitVisualOnlyFallback() {
        LegacyGuiItemPlan plan = factory.planItem("REINFORCED_DEEPSLATE", MaterialContext.GUI_ICON,
                "&cUnavailable visual", Collections.<String>emptyList(), null);
        assertEquals(Material.STONE, plan.getMaterial());
        assertTrue(plan.isVisualFallback());
    }

    @Test
    void rawCreationCannotHideVisualFallback() {
        assertThrows(IllegalArgumentException.class, () -> factory.createItem(
                "REINFORCED_DEEPSLATE", MaterialContext.GUI_ICON, "visual",
                Collections.<String>emptyList(), null));
    }

    @Test
    void rejectsUnsupportedGameplayCriticalMaterial() {
        assertThrows(IllegalArgumentException.class, () -> factory.planItem(
                "REINFORCED_DEEPSLATE", MaterialContext.REQUIRED_ITEM, "x",
                Collections.<String>emptyList(), null));
    }

    @Test
    void explicitlyOmitsVisualCustomModelData() {
        LegacyGuiItemPlan plan = factory.planItem("ARROW", MaterialContext.GUI_ICON, "next",
                Collections.<String>emptyList(), Integer.valueOf(42));
        assertEquals(Material.ARROW, plan.getMaterial());
        assertTrue(plan.isCustomModelDataOmitted());
    }

    @Test
    void plansLegacyPlayerSkullWithOwner() {
        LegacyGuiItemPlan plan = factory.planPlayerHead("Steve", "&aSteve", Arrays.asList("&7Trusted"));
        assertEquals(Material.SKULL_ITEM, plan.getMaterial());
        assertEquals(3, plan.getData());
        assertEquals("Steve", plan.getSkullOwner());
        assertEquals("\u00a7aSteve", plan.getDisplayName());
        assertEquals(Collections.singletonList("\u00a77Trusted"), plan.getLore());
    }

    @Test
    void omitsUnavailableOwnerWithoutChangingSkullRepresentation() {
        LegacyGuiItemPlan plan = factory.planPlayerHead(" ", "Player", Collections.<String>emptyList());
        assertEquals(Material.SKULL_ITEM, plan.getMaterial());
        assertEquals(3, plan.getData());
        assertEquals(null, plan.getSkullOwner());
    }

    @Test
    void customTextureInjectionIsExplicitlyUnsupported() {
        assertFalse(factory.supportsCustomTextureHeads());
    }
}
