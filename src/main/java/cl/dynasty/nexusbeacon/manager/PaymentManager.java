package cl.dynasty.nexusbeacon.manager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import cl.dynasty.nexusbeacon.NexusBeaconPlugin;
import cl.dynasty.nexusbeacon.effects.BeaconEffect;
import cl.dynasty.nexusbeacon.util.DebugLogger;

public class PaymentManager {

    private final NexusBeaconPlugin plugin;

    public PaymentManager(NexusBeaconPlugin plugin) {
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

        if (options == null) {
            return plugin.getLanguageManager().get("payment.free");
        }

        List<String> parts = new ArrayList<>();

        for (String key : options.getKeys(false)) {
            ConfigurationSection option = options.getConfigurationSection(key);

            if (option == null) {
                continue;
            }

            String type = option.getString("type", "NONE");
            int amount = option.getInt("amount", option.getInt("amount-per-level", 0) * level);

            if (type.equalsIgnoreCase("ITEM")) {
                String materialName = option.getString("material", "DIAMOND");
                parts.add(amount + "x " + getMaterialName(materialName));
                continue;
            }

            if (type.equalsIgnoreCase("EXP_LEVEL")) {
                parts.add(amount + " EXP");
                continue;
            }

            if (type.equalsIgnoreCase("VAULT_MONEY")) {
                parts.add("$" + amount);
            }
        }

        if (parts.isEmpty()) {
            return plugin.getLanguageManager().get("payment.free");
        }

        String separator = plugin.getLanguageManager().get("payment.cost-separator");

        return plugin.getLanguageManager().get(
                "payment.cost-multiple",
                Map.of("cost", join(parts, separator)));
    }

    public String getOptionText(BeaconEffect effect, String action, String optionKey, int level) {
        ConfigurationSection section = getLevelOption(effect, action, optionKey, level);

        if (section == null) {
            return plugin.getLanguageManager().get("payment.option-not-configured");
        }

        String type = section.getString("type", "NONE");
        int amount = section.getInt("amount", section.getInt("amount-per-level", 0) * level);

        if (type.equalsIgnoreCase("ITEM")) {
            String materialName = section.getString("material", "DIAMOND");

            return plugin.getLanguageManager().get(
                    "payment.cost-item",
                    Map.of(
                            "amount", String.valueOf(amount),
                            "material", getMaterialName(materialName)));
        }

        if (type.equalsIgnoreCase("EXP_LEVEL")) {
            return plugin.getLanguageManager().get(
                    "payment.cost-exp",
                    Map.of("amount", String.valueOf(amount)));
        }

        if (type.equalsIgnoreCase("VAULT_MONEY")) {
            return plugin.getLanguageManager().get(
                    "payment.cost-money",
                    Map.of("amount", String.valueOf(amount)));
        }

        return plugin.getLanguageManager().get("payment.free");
    }

    public boolean payOption(Player player, BeaconEffect effect, String action, String optionKey, int level) {
        ConfigurationSection section = getLevelOption(effect, action, optionKey, level);

        if (section == null) {
            DebugLogger.log(plugin, "payment-missing:" + effect.getId() + ":" + action + ":" + optionKey,
                    "Payment option missing effect=" + effect.getId()
                            + " action=" + action
                            + " option=" + optionKey
                            + " level=" + level);

            player.sendMessage(plugin.getLanguageManager().withPrefix("payment.payment-option-not-configured"));
            return false;
        }

        DebugLogger.log(plugin, "payment:" + effect.getId() + ":" + action + ":" + optionKey,
                "Payment option effect=" + effect.getId()
                        + " action=" + action
                        + " option=" + optionKey
                        + " level=" + level
                        + " type=" + section.getString("type", "NONE"));

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
                player.sendMessage(plugin.getLanguageManager().withPrefix(
                        "payment.need-exp",
                        Map.of("amount", String.valueOf(amount))));
                return false;
            }

            player.setLevel(player.getLevel() - amount);
            return true;
        }

        if (type.equalsIgnoreCase("ITEM")) {
            String materialName = section.getString("material", "DIAMOND");
            Material material = plugin.getVersionAdapter().material(materialName);

            if (!hasItem(player, material, amount)) {
                player.sendMessage(plugin.getLanguageManager().withPrefix(
                        "payment.need-item",
                        Map.of(
                                "amount", String.valueOf(amount),
                                "material", getMaterialName(materialName))));
                return false;
            }

            removeItem(player, material, amount);
            return true;
        }

        if (type.equalsIgnoreCase("VAULT_MONEY")) {
            if (plugin.getEconomy() == null) {
                player.sendMessage(plugin.getLanguageManager().withPrefix("payment.vault-unavailable"));
                return false;
            }

            if (!plugin.getEconomy().has(player, amount)) {
                player.sendMessage(plugin.getLanguageManager().withPrefix(
                        "payment.need-money",
                        Map.of("amount", String.valueOf(amount))));
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
                player.sendMessage(plugin.getLanguageManager().withPrefix(
                        "payment.need-exp",
                        Map.of("amount", String.valueOf(amount))));
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
                player.sendMessage(plugin.getLanguageManager().withPrefix(
                        "payment.need-item",
                        Map.of(
                                "amount", String.valueOf(amount),
                                "material", getMaterialName(materialName))));
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
        return plugin.getLanguageManager().materialName(material.toUpperCase());
    }
}