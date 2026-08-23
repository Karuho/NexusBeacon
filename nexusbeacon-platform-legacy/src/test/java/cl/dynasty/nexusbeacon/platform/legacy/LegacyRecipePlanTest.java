package cl.dynasty.nexusbeacon.platform.legacy;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class LegacyRecipePlanTest {
    @Test void preservesBundledModernShapeIngredientsAndAmount() {
        LegacyRecipePlan plan = LegacyRecipePlan.parse(defaultRecipe(), new LegacyMaterialResolver());

        assertArrayEquals(new String[] { "DDD", "DBD", "OOO" }, plan.getShape());
        assertEquals(1, plan.getResultAmount());
        assertIngredient(plan, 'D', Material.DIAMOND_BLOCK, 0, LegacyMaterialMappingKind.EXACT);
        assertIngredient(plan, 'B', Material.BEACON, 0, LegacyMaterialMappingKind.EXACT);
        assertIngredient(plan, 'O', Material.OBSIDIAN, 0, LegacyMaterialMappingKind.EXACT);
    }

    @Test void preservesLegacyAliasDataValueThroughPhaseNineResolver() {
        YamlConfiguration recipe = defaultRecipe();
        recipe.set("ingredients.D", "LIGHT_BLUE_DYE");

        LegacyRecipePlan plan = LegacyRecipePlan.parse(recipe, new LegacyMaterialResolver());

        assertIngredient(plan, 'D', Material.INK_SACK, 12, LegacyMaterialMappingKind.COMPATIBLE_ALIAS);
    }

    @Test void rejectsUnsupportedIngredientWithoutFallback() {
        YamlConfiguration recipe = defaultRecipe();
        recipe.set("ingredients.D", "NETHERITE_BLOCK");

        LegacyRecipeValidationException failure = assertThrows(LegacyRecipeValidationException.class,
                () -> LegacyRecipePlan.parse(recipe, new LegacyMaterialResolver()));
        assertEquals(LegacyRecipeRegistrationStatus.UNSUPPORTED_INGREDIENT, failure.getStatus());
    }

    @Test void rejectsMissingShapeIngredientInsteadOfCreatingPartialRecipe() {
        YamlConfiguration recipe = defaultRecipe();
        recipe.set("ingredients.B", null);

        LegacyRecipeValidationException failure = assertThrows(LegacyRecipeValidationException.class,
                () -> LegacyRecipePlan.parse(recipe, new LegacyMaterialResolver()));
        assertEquals(LegacyRecipeRegistrationStatus.INVALID_CONFIG, failure.getStatus());
    }

    @Test void rejectsUnequalRowsAndUnusedSymbols() {
        YamlConfiguration rows = defaultRecipe();
        rows.set("shape", Arrays.asList("DDD", "DB", "OOO"));
        assertThrows(LegacyRecipeValidationException.class,
                () -> LegacyRecipePlan.parse(rows, new LegacyMaterialResolver()));

        YamlConfiguration unused = defaultRecipe();
        unused.set("ingredients.X", "STONE");
        assertThrows(LegacyRecipeValidationException.class,
                () -> LegacyRecipePlan.parse(unused, new LegacyMaterialResolver()));
    }

    @Test void returnedCollectionsAreImmutableAndShapeIsDefensive() {
        LegacyRecipePlan plan = LegacyRecipePlan.parse(defaultRecipe(), new LegacyMaterialResolver());
        String[] shape = plan.getShape();
        shape[0] = "XXX";
        assertEquals("DDD", plan.getShape()[0]);
        assertThrows(UnsupportedOperationException.class,
                () -> plan.getIngredients().clear());
    }

    private static YamlConfiguration defaultRecipe() {
        YamlConfiguration recipe = new YamlConfiguration();
        recipe.set("enabled", Boolean.TRUE);
        recipe.set("shape", Arrays.asList("DDD", "DBD", "OOO"));
        recipe.set("ingredients.B", "BEACON");
        recipe.set("ingredients.D", "DIAMOND_BLOCK");
        recipe.set("ingredients.O", "OBSIDIAN");
        return recipe;
    }

    private static void assertIngredient(LegacyRecipePlan plan, char symbol, Material material,
            int data, LegacyMaterialMappingKind mapping) {
        LegacyRecipeIngredient ingredient = plan.getIngredients().get(Character.valueOf(symbol));
        assertEquals(material, ingredient.getMaterial());
        assertEquals(data, ingredient.getData());
        assertEquals(mapping, ingredient.getMappingKind());
    }
}
