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
    removeCustomRecipe();
    removeVanillaBeaconRecipe();

    if (!plugin.getConfigManager().getBeaconConfig().getBoolean("recipe.enabled", true)) {
        return;
    }

    ConfigurationSection recipeSection = plugin.getConfigManager()
            .getBeaconConfig()
            .getConfigurationSection("recipe");

    if (recipeSection == null) {
        return;
    }

    java.util.List<String> shape = recipeSection.getStringList("shape");

    if (shape.size() != 3) {
        plugin.getLogger().warning("[NexusBeacon] Invalid recipe shape in beacon.yml. Expected 3 rows.");
        return;
    }

    ItemStack result = plugin.getCustomBeaconItemManager().createBeaconItem(1);

    org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(plugin, "nexusbeacon");
    ShapedRecipe recipe = new ShapedRecipe(key, result);

    recipe.shape(
            shape.get(0),
            shape.get(1),
            shape.get(2));

    ConfigurationSection ingredients = recipeSection.getConfigurationSection("ingredients");

    if (ingredients == null) {
        plugin.getLogger().warning("[NexusBeacon] Missing recipe.ingredients in beacon.yml.");
        return;
    }

    for (String symbol : ingredients.getKeys(false)) {
        if (symbol.length() != 1) {
            plugin.getLogger().warning("[NexusBeacon] Invalid recipe symbol in beacon.yml: " + symbol);
            continue;
        }

        String materialName = ingredients.getString(symbol);
        Material material = plugin.getVersionAdapter().material(materialName);

        if (material == null) {
            plugin.getLogger().warning("[NexusBeacon] Invalid recipe material in beacon.yml: " + materialName);
            continue;
        }

        recipe.setIngredient(symbol.charAt(0), material);
    }

    Bukkit.addRecipe(recipe);
    plugin.getLogger().info(plugin.getLanguageManager().get("console.custom-recipe-registered"));
}

private void removeCustomRecipe() {
    org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(plugin, "nexusbeacon");
    Bukkit.removeRecipe(key);
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