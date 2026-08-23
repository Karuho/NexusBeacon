package cl.dynasty.nexusbeacon.platform.legacy;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.configuration.ConfigurationSection;

import cl.dynasty.nexusbeacon.platform.api.MaterialContext;

public final class LegacyRecipePlan {
    private final String[] shape;
    private final Map<Character, LegacyRecipeIngredient> ingredients;
    private final int resultAmount;

    private LegacyRecipePlan(String[] shape, Map<Character, LegacyRecipeIngredient> ingredients,
            int resultAmount) {
        this.shape = shape.clone();
        this.ingredients = Collections.unmodifiableMap(
                new LinkedHashMap<Character, LegacyRecipeIngredient>(ingredients));
        this.resultAmount = resultAmount;
    }

    public static LegacyRecipePlan parse(ConfigurationSection recipe, LegacyMaterialResolver materials) {
        if (recipe == null) {
            throw invalid("Missing recipe section");
        }
        List<String> configuredShape = recipe.getStringList("shape");
        if (configuredShape.size() != 3) {
            throw invalid("Recipe shape must contain exactly 3 rows");
        }
        String[] shape = configuredShape.toArray(new String[configuredShape.size()]);
        int width = shape[0] == null ? 0 : shape[0].length();
        if (width < 1 || width > 3) throw invalid("Recipe rows must contain 1 to 3 symbols");
        Set<Character> usedSymbols = new LinkedHashSet<Character>();
        for (String row : shape) {
            if (row == null || row.length() != width) {
                throw invalid("Recipe rows must have equal width");
            }
            for (char symbol : row.toCharArray()) {
                if (symbol != ' ') usedSymbols.add(Character.valueOf(symbol));
            }
        }
        if (usedSymbols.isEmpty()) throw invalid("Recipe shape has no ingredients");

        ConfigurationSection configuredIngredients = recipe.getConfigurationSection("ingredients");
        if (configuredIngredients == null || configuredIngredients.getKeys(false).isEmpty()) {
            throw invalid("Missing recipe.ingredients");
        }
        Map<Character, LegacyRecipeIngredient> ingredients =
                new LinkedHashMap<Character, LegacyRecipeIngredient>();
        for (String key : configuredIngredients.getKeys(false)) {
            if (key == null || key.length() != 1 || key.charAt(0) == ' ') {
                throw invalid("Recipe ingredient symbols must be one non-space character: " + key);
            }
            Character symbol = Character.valueOf(key.charAt(0));
            if (!usedSymbols.contains(symbol)) {
                throw invalid("Recipe ingredient symbol is not used by shape: " + key);
            }
            String identifier = configuredIngredients.getString(key);
            LegacyMaterialResolution resolution = materials.resolveLegacyMaterial(
                    identifier, MaterialContext.RECIPE_INGREDIENT);
            if (!resolution.getResolution().isResolved()) {
                throw new LegacyRecipeValidationException(
                        LegacyRecipeRegistrationStatus.UNSUPPORTED_INGREDIENT,
                        "Unsupported recipe ingredient " + key + ": " + identifier);
            }
            ingredients.put(symbol, new LegacyRecipeIngredient(key.charAt(0), identifier,
                    resolution.getResolution().getMaterial().get(), resolution.getData(),
                    resolution.getMappingKind()));
        }
        if (!ingredients.keySet().containsAll(usedSymbols)) {
            Set<Character> missing = new LinkedHashSet<Character>(usedSymbols);
            missing.removeAll(ingredients.keySet());
            throw invalid("Recipe shape symbols have no ingredient: " + missing);
        }
        return new LegacyRecipePlan(shape, ingredients, 1);
    }

    private static LegacyRecipeValidationException invalid(String message) {
        return new LegacyRecipeValidationException(LegacyRecipeRegistrationStatus.INVALID_CONFIG, message);
    }

    public String[] getShape() { return shape.clone(); }
    public Map<Character, LegacyRecipeIngredient> getIngredients() { return ingredients; }
    public int getResultAmount() { return resultAmount; }
}
