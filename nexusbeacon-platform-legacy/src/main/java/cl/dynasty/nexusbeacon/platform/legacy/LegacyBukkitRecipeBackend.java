package cl.dynasty.nexusbeacon.platform.legacy;

import java.util.Iterator;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Recipe;

final class LegacyBukkitRecipeBackend implements LegacyRecipeBackend {
    @Override public Iterator<Recipe> recipes() { return Bukkit.recipeIterator(); }
    @Override public boolean add(Recipe recipe) { return Bukkit.addRecipe(recipe); }
}
