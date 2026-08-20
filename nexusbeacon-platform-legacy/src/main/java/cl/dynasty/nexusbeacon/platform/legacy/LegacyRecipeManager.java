package cl.dynasty.nexusbeacon.platform.legacy;

import java.util.Iterator;
import java.util.Map;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;

/** Bukkit-only recipe lifecycle compatible with both pre-keyed and keyed Legacy servers. */
public final class LegacyRecipeManager {
    private final LegacyMaterialResolver materials;
    private final RecipeResultFactory results;
    private final LegacyItemIdentityService identities;
    private final LegacyRecipeBackend backend;
    private LegacyRecipePlan activePlan;
    private LegacyRecipeRegistrationResult lastResult = LegacyRecipeRegistrationResult.of(
            LegacyRecipeRegistrationStatus.DISABLED, "Not initialized");

    public LegacyRecipeManager(LegacyMaterialResolver materials, LegacyBeaconItemFactory items,
            LegacyItemIdentityService identities) {
        this(materials, new RecipeResultFactory() {
            @Override public ItemStack create(int amount) { return items.createNew(amount); }
        }, identities, new LegacyBukkitRecipeBackend());
    }

    LegacyRecipeManager(LegacyMaterialResolver materials, RecipeResultFactory results,
            LegacyItemIdentityService identities, LegacyRecipeBackend backend) {
        if (materials == null) throw new NullPointerException("materials");
        if (results == null) throw new NullPointerException("results");
        if (identities == null) throw new NullPointerException("identities");
        if (backend == null) throw new NullPointerException("backend");
        this.materials = materials;
        this.results = results;
        this.identities = identities;
        this.backend = backend;
    }

    public LegacyRecipeRegistrationResult register(ConfigurationSection recipeSection) {
        if (recipeSection == null) {
            return remember(LegacyRecipeRegistrationStatus.INVALID_CONFIG, "Missing recipe section");
        }
        if (!recipeSection.getBoolean("enabled", true)) {
            activePlan = null;
            return remember(LegacyRecipeRegistrationStatus.DISABLED, "Recipe disabled by configuration");
        }
        final LegacyRecipePlan plan;
        try {
            plan = LegacyRecipePlan.parse(recipeSection, materials);
        } catch (LegacyRecipeValidationException exception) {
            return remember(exception.getStatus(), exception.getMessage());
        } catch (RuntimeException exception) {
            return remember(LegacyRecipeRegistrationStatus.INVALID_CONFIG, exception.getMessage());
        }

        try {
            if (findEquivalent(plan) != null) {
                activePlan = plan;
                return remember(LegacyRecipeRegistrationStatus.ALREADY_PRESENT,
                        "Equivalent marked NexusBeacon recipe already present");
            }
            ShapedRecipe recipe = build(plan, results.create(plan.getResultAmount()));
            if (!backend.add(recipe)) {
                return remember(LegacyRecipeRegistrationStatus.BUKKIT_REJECTED,
                        "Bukkit rejected recipe registration");
            }
            activePlan = plan;
            return remember(LegacyRecipeRegistrationStatus.REGISTERED,
                    "Legacy NexusBeacon recipe registered");
        } catch (RuntimeException exception) {
            return remember(LegacyRecipeRegistrationStatus.BUKKIT_REJECTED,
                    exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage());
        }
    }

    public int unregister() {
        if (activePlan == null) return 0;
        int removed = 0;
        Iterator<Recipe> iterator = backend.recipes();
        while (iterator.hasNext()) {
            Recipe recipe = iterator.next();
            if (isEquivalent(recipe, activePlan)) {
                try {
                    iterator.remove();
                    removed++;
                } catch (UnsupportedOperationException unsupported) {
                    // 1.12 exposes its keyed recipe registry through an unmodifiable iterator
                    // and has no public remove-by-key API. Leaving this exact recipe in place is
                    // safer than clearing global recipes; the next enable detects it semantically.
                    break;
                }
            }
        }
        activePlan = null;
        return removed;
    }

    public LegacyRecipeRegistrationResult getLastResult() { return lastResult; }

    private Recipe findEquivalent(LegacyRecipePlan plan) {
        Iterator<Recipe> recipes = backend.recipes();
        while (recipes.hasNext()) {
            Recipe candidate = recipes.next();
            if (isEquivalent(candidate, plan)) return candidate;
        }
        return null;
    }

    private boolean isEquivalent(Recipe candidate, LegacyRecipePlan plan) {
        if (!(candidate instanceof ShapedRecipe)) return false;
        ShapedRecipe shaped = (ShapedRecipe) candidate;
        ItemStack result = shaped.getResult();
        if (result == null || result.getAmount() != plan.getResultAmount()
                || !identities.identify(result).isRecognized()) return false;
        String[] expectedShape = plan.getShape();
        String[] actualShape = shaped.getShape();
        if (expectedShape.length != actualShape.length) return false;
        Map<Character, ItemStack> actual = shaped.getIngredientMap();
        Map<Character, LegacyRecipeIngredient> expected = plan.getIngredients();
        for (int row = 0; row < expectedShape.length; row++) {
            if (expectedShape[row].length() != actualShape[row].length()) return false;
            for (int column = 0; column < expectedShape[row].length(); column++) {
                LegacyRecipeIngredient expectedIngredient = expected.get(expectedShape[row].charAt(column));
                ItemStack actualIngredient = actual.get(actualShape[row].charAt(column));
                if (expectedIngredient == null || actualIngredient == null
                        || actualIngredient.getType() != expectedIngredient.getMaterial()
                        || actualIngredient.getDurability() != expectedIngredient.getData()) return false;
            }
        }
        return true;
    }

    private ShapedRecipe build(LegacyRecipePlan plan, ItemStack result) {
        ShapedRecipe recipe = new ShapedRecipe(result);
        recipe.shape(plan.getShape());
        for (LegacyRecipeIngredient ingredient : plan.getIngredients().values()) {
            recipe.setIngredient(ingredient.getSymbol(), ingredient.getMaterial(), ingredient.getData());
        }
        return recipe;
    }

    private LegacyRecipeRegistrationResult remember(LegacyRecipeRegistrationStatus status, String diagnostic) {
        lastResult = LegacyRecipeRegistrationResult.of(status, diagnostic);
        return lastResult;
    }

    interface RecipeResultFactory {
        ItemStack create(int amount);
    }
}
