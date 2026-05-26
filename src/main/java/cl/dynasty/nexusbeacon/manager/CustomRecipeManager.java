package cl.dynasty.nexusbeacon.manager;

import java.util.Iterator;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;

import cl.dynasty.nexusbeacon.NexusBeaconPlugin;

public class CustomRecipeManager {

    private final NexusBeaconPlugin plugin;

    public CustomRecipeManager(NexusBeaconPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        removeVanillaBeaconRecipe();

        if (!plugin.getConfigManager().getBeaconConfig().getBoolean("recipe.enabled", true)) {
            return;
        }

        ItemStack result = plugin.getCustomBeaconItemManager().createBeaconItem(1);
        ShapedRecipe recipe;

        try {
            org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(plugin, "NexusBeacon");
            recipe = new ShapedRecipe(key, result);
        } catch (Throwable throwable) {
            recipe = new ShapedRecipe(result); // fallback legacy
        }

        recipe.shape(
                plugin.getConfigManager().getBeaconConfig().getString("recipe.shape.0", "DND"),
                plugin.getConfigManager().getBeaconConfig().getString("recipe.shape.1", "DBD"),
                plugin.getConfigManager().getBeaconConfig().getString("recipe.shape.2", "OGO"));

        ConfigurationSection ingredients = plugin.getConfigManager()
                .getBeaconConfig()
                .getConfigurationSection("recipe.ingredients");

        if (ingredients != null) {
            for (String key : ingredients.getKeys(false)) {
                String materialName = ingredients.getString(key);
                Material material = plugin.getVersionAdapter().material(materialName);

                if (material != null) {
                    recipe.setIngredient(key.charAt(0), material);
                }
            }
        }

        Bukkit.addRecipe(recipe);
        plugin.getLogger().info(plugin.getLanguageManager().get("console.custom-recipe-registered"));
    }

    public void removeVanillaBeaconRecipe() {
        if (!plugin.getConfigManager().getBeaconConfig().getBoolean("vanilla-beacon.disable-recipe", false)) {
            return;
        }

        Iterator<Recipe> iterator = Bukkit.recipeIterator();

        while (iterator.hasNext()) {
            Recipe recipe = iterator.next();

            if (recipe != null
                    && recipe.getResult() != null
                    && recipe.getResult().getType() == Material.BEACON
                    && !plugin.getCustomBeaconItemManager().isCustomBeacon(recipe.getResult())) {
                iterator.remove();
            }
        }

        plugin.getLogger().info(plugin.getLanguageManager().get("console.vanilla-recipe-disabled"));
    }
}