package cl.dynasty.nexusbeacon.platform.legacy;

import java.util.Iterator;
import org.bukkit.inventory.Recipe;

interface LegacyRecipeBackend {
    Iterator<Recipe> recipes();
    boolean add(Recipe recipe);
}
