package cl.dynasty.dynabeacon.manager;

import cl.dynasty.dynabeacon.DynaBeaconPlugin;
import cl.dynasty.dynabeacon.effects.BeaconEffect;
import cl.dynasty.dynabeacon.util.ColorUtil;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class PaymentManager {

    private final DynaBeaconPlugin plugin;

    public PaymentManager(DynaBeaconPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean payAcquire(Player player, BeaconEffect effect) {
        return pay(player, effect, "acquire", 1);
    }

    public boolean payUpgrade(Player player, BeaconEffect effect, int nextLevel) {
        return pay(player, effect, "upgrade", nextLevel);
    }

    public String getCostText(BeaconEffect effect, String path, int level) {
        ConfigurationSection options = plugin.getConfigManager()
                .getEffectsConfig()
                .getConfigurationSection("effects." + effect.getId() + ".costs." + path + ".options");

        if (options == null)
            return "&7Costo: &aGratis";

        java.util.List<String> parts = new java.util.ArrayList<String>();

        for (String key : options.getKeys(false)) {
            ConfigurationSection option = options.getConfigurationSection(key);
            if (option == null)
                continue;

            String type = option.getString("type", "NONE");
            int amount = option.getInt("amount", option.getInt("amount-per-level", 0) * level);

            if (type.equalsIgnoreCase("ITEM")) {
                String materialName = option.getString("material", "DIAMOND");
                parts.add(amount + "x " + getMaterialName(materialName));
            } else if (type.equalsIgnoreCase("EXP_LEVEL")) {
                parts.add(amount + " EXP");
            } else if (type.equalsIgnoreCase("VAULT_MONEY")) {
                parts.add("$" + amount);
            }
        }

        if (parts.isEmpty())
            return "&7Costo: &aGratis";

        return "&7Costo: &e" + join(parts, " &7/ &e");
    }

    public String getOptionText(BeaconEffect effect, String action, String optionKey, int level) {
        ConfigurationSection section = getLevelOption(effect, action, optionKey, level);

        if (section == null) {
            return ColorUtil.color("&cOpción no configurada.");
        }

        String type = section.getString("type", "NONE");
        int amount = section.getInt("amount", section.getInt("amount-per-level", 0) * level);

        if (type.equalsIgnoreCase("ITEM")) {
            String materialName = section.getString("material", "DIAMOND");
            return ColorUtil.color("&7Costo: &b" + amount + "x " + getMaterialName(materialName));
        }

        if (type.equalsIgnoreCase("EXP_LEVEL")) {
            return ColorUtil.color("&7Costo: &a" + amount + " niveles EXP");
        }

        if (type.equalsIgnoreCase("VAULT_MONEY")) {
            return ColorUtil.color("&7Costo: &6$" + amount);
        }

        return ColorUtil.color("&7Costo: &aGratis");
    }

    public boolean payOption(Player player, BeaconEffect effect, String action, String optionKey, int level) {
        ConfigurationSection section = getLevelOption(effect, action, optionKey, level);

        if (section == null) {
            player.sendMessage(ColorUtil.color("&b[DynaBeacon]&r &cEsa opción de pago no está configurada."));
            return false;
        }

        return paySection(player, section, level);
    }

    private ConfigurationSection getOption(BeaconEffect effect, String action, String optionKey) {
        return plugin.getConfigManager()
                .getEffectsConfig()
                .getConfigurationSection("effects." + effect.getId() + ".costs." + action + ".options." + optionKey);
    }

    private ConfigurationSection getLevelOption(BeaconEffect effect, String action, String optionKey, int level) {
        ConfigurationSection levelOption = plugin.getConfigManager()
                .getEffectsConfig()
                .getConfigurationSection("effects." + effect.getId()
                        + ".levels." + level
                        + ".costs." + action
                        + ".options." + optionKey);

        if (levelOption != null) {
            return levelOption;
        }

        return getOption(effect, action, optionKey);
    }

    private boolean paySection(Player player, ConfigurationSection section, int level) {
        String type = section.getString("type", "NONE");
        int amount = section.getInt("amount", section.getInt("amount-per-level", 0) * level);

        if (type.equalsIgnoreCase("NONE")) {
            return true;
        }

        if (type.equalsIgnoreCase("EXP_LEVEL")) {
            if (player.getLevel() < amount) {
                player.sendMessage(
                        ColorUtil.color("&b[DynaBeacon]&r &cNecesitas &f" + amount + " &cniveles de experiencia."));
                return false;
            }

            player.setLevel(player.getLevel() - amount);
            return true;
        }

        if (type.equalsIgnoreCase("ITEM")) {
            String materialName = section.getString("material", "DIAMOND");
            Material material = plugin.getVersionAdapter().material(materialName);

            if (!hasItem(player, material, amount)) {
                player.sendMessage(ColorUtil.color(
                        "&b[DynaBeacon]&r &cNecesitas &f" + amount + "x " + getMaterialName(materialName) + "&c."));
                return false;
            }

            removeItem(player, material, amount);
            return true;
        }

        if (type.equalsIgnoreCase("VAULT_MONEY")) {
            if (plugin.getEconomy() == null) {
                player.sendMessage(ColorUtil.color("&b[DynaBeacon]&r &cLa economía Vault no está disponible."));
                return false;
            }

            if (!plugin.getEconomy().has(player, amount)) {
                player.sendMessage(ColorUtil.color("&b[DynaBeacon]&r &cNecesitas &f$" + amount + "&c."));
                return false;
            }

            plugin.getEconomy().withdrawPlayer(player, amount);
            return true;
        }

        return false;
    }

    private String join(java.util.List<String> list, String separator) {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < list.size(); i++) {
            if (i > 0)
                builder.append(separator);
            builder.append(list.get(i));
        }

        return builder.toString();
    }

    private boolean pay(Player player, BeaconEffect effect, String path, int level) {
        ConfigurationSection section = plugin.getConfigManager()
                .getEffectsConfig()
                .getConfigurationSection("effects." + effect.getId() + ".costs." + path);

        if (section == null) {
            return true;
        }

        String type = section.getString("type", "NONE");

        if (type.equalsIgnoreCase("NONE")) {
            return true;
        }

        if (type.equalsIgnoreCase("EXP_LEVEL")) {
            int amount = section.getInt("amount", section.getInt("amount-per-level", 0) * level);

            if (player.getLevel() < amount) {
                player.sendMessage("§b[DynaBeacon] §cNecesitas §f" + amount + " §cniveles de experiencia.");
                return false;
            }

            player.setLevel(player.getLevel() - amount);
            return true;
        }

        if (type.equalsIgnoreCase("ITEM")) {
            String materialName = section.getString("material", "DIAMOND");
            int amount = section.getInt("amount", section.getInt("amount-per-level", 0) * level);

            Material material = plugin.getVersionAdapter().material(materialName);

            if (!hasItem(player, material, amount)) {
                player.sendMessage(
                        "§b[DynaBeacon] §cNecesitas §f" + amount + "x " + getMaterialName(materialName) + "§c.");
                return false;
            }

            removeItem(player, material, amount);
            return true;
        }

        return true;
    }

    private boolean hasItem(Player player, Material material, int amount) {
        if (amount <= 0)
            return true;

        return player.getInventory().containsAtLeast(new ItemStack(material), amount);
    }

    private void removeItem(Player player, Material material, int amount) {
        if (amount <= 0)
            return;

        player.getInventory().removeItem(new ItemStack(material, amount));
        player.updateInventory();
    }

    private String getMaterialName(String material) {
        return plugin.getConfigManager()
                .getLanguageConfig()
                .getString("materials." + material.toUpperCase(), material);
    }
}