package cl.dynasty.nexusbeacon.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import cl.dynasty.nexusbeacon.NexusBeaconPlugin;
import cl.dynasty.nexusbeacon.model.BeaconData;

public class NexusBeaconCommand implements CommandExecutor, TabCompleter {

    private final NexusBeaconPlugin plugin;

    public NexusBeaconCommand(NexusBeaconPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender, label);
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("NexusBeacon.admin")) {
                sender.sendMessage(plugin.getLanguageManager().withPrefix("no-permission"));
                return true;
            }

            plugin.reloadAll();
            plugin.restartRuntimeTasks();
            sender.sendMessage(plugin.getLanguageManager().withPrefix("reload"));
            return true;
        }

        if (args[0].equalsIgnoreCase("give")) {
            if (!sender.hasPermission("NexusBeacon.admin")) {
                sender.sendMessage(plugin.getLanguageManager().withPrefix("no-permission"));
                return true;
            }

            if (args.length < 2) {
                sender.sendMessage(plugin.getLanguageManager().withPrefix(
                        "command.give-usage",
                        Map.of("label", label)));
                return true;
            }

            Player target = Bukkit.getPlayer(args[1]);

            if (target == null) {
                sender.sendMessage(plugin.getLanguageManager().withPrefix("player-not-found"));
                return true;
            }

            int amount = 1;

            if (args.length >= 3) {
                try {
                    amount = Integer.parseInt(args[2]);
                } catch (NumberFormatException ignored) {
                    amount = 1;
                }
            }

            if (amount < 1)
                amount = 1;
            if (amount > 64)
                amount = 64;

            ItemStack item = plugin.getCustomBeaconItemManager().createBeaconItem(amount);
            target.getInventory().addItem(item);

            sender.sendMessage(plugin.getLanguageManager().withPrefix(
                    "command.give-sender",
                    Map.of(
                            "amount", String.valueOf(amount),
                            "player", target.getName())));
            target.sendMessage(plugin.getLanguageManager().withPrefix("command.give-target"));
            return true;
        }

        if (args[0].equalsIgnoreCase("trust")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(plugin.getLanguageManager().withPrefix("only-players"));
                return true;
            }

            Player player = (Player) sender;

            if (args.length < 2) {
                player.sendMessage(plugin.getLanguageManager().withPrefix(
                        "command.trust-usage",
                        Map.of("label", label)));
                return true;
            }

            BeaconData beacon = getTargetBeacon(player);

            if (beacon == null) {
                player.sendMessage(plugin.getLanguageManager().withPrefix("command.look-at-beacon"));
                return true;
            }

            if (!beacon.canManage(player.getUniqueId()) && !player.hasPermission("NexusBeacon.admin")) {
                player.sendMessage(plugin.getLanguageManager().withPrefix("command.cannot-manage"));
                return true;
            }

            Player target = Bukkit.getPlayer(args[1]);

            if (target == null) {
                player.sendMessage(plugin.getLanguageManager().withPrefix("command.player-not-online"));
                return true;
            }

            if (beacon.getOwner().equals(target.getUniqueId())) {
                player.sendMessage(plugin.getLanguageManager().withPrefix("trust.cannot-trust-owner"));
                return true;
            }

            beacon.addTrusted(target.getUniqueId());
            plugin.getStorageManager().saveBeacon(beacon);

            player.sendMessage(plugin.getLanguageManager().withPrefix(
                    "command.trusted-added",
                    Map.of("player", target.getName())));
            return true;
        }

        if (args[0].equalsIgnoreCase("untrust")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(plugin.getLanguageManager().withPrefix("only-players"));
                return true;
            }

            Player player = (Player) sender;

            if (args.length < 2) {
                player.sendMessage(plugin.getLanguageManager().withPrefix(
                        "command.untrust-usage",
                        Map.of("label", label)));
                return true;
            }

            BeaconData beacon = getTargetBeacon(player);

            if (beacon == null) {
                player.sendMessage(plugin.getLanguageManager().withPrefix("command.look-at-beacon"));
                return true;
            }

            if (!beacon.canManage(player.getUniqueId()) && !player.hasPermission("NexusBeacon.admin")) {
                player.sendMessage(plugin.getLanguageManager().withPrefix("command.cannot-manage"));
                return true;
            }

            Player target = Bukkit.getPlayer(args[1]);

            if (target == null) {
                player.sendMessage(plugin.getLanguageManager().withPrefix("command.player-not-online"));
                return true;
            }

            beacon.removeTrusted(target.getUniqueId());
            plugin.getStorageManager().saveBeacon(beacon);

            player.sendMessage(plugin.getLanguageManager().withPrefix(
                    "command.trusted-removed",
                    Map.of("player", target.getName())));
            return true;
        }

        if (args[0].equalsIgnoreCase("trusted")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(plugin.getLanguageManager().withPrefix("only-players"));
                return true;
            }

            Player player = (Player) sender;
            BeaconData beacon = getTargetBeacon(player);

            if (beacon == null) {
                player.sendMessage(plugin.getLanguageManager().withPrefix("command.look-at-beacon"));
                return true;
            }

            player.sendMessage(plugin.getLanguageManager().withPrefix(
                    "command.trusted-count",
                    Map.of("amount", String.valueOf(beacon.getTrustedPlayers().size()))));
            return true;
        }

        if (args[0].equalsIgnoreCase("storage")) {
            if (!sender.hasPermission("NexusBeacon.admin")) {
                sender.sendMessage(plugin.getLanguageManager().withPrefix("no-permission"));
                return true;
            }

            return handleStorageCommand(sender, label, args);
        }

        sender.sendMessage(plugin.getLanguageManager().withPrefix(
                "command.unknown",
                Map.of("label", label)));
        return true;
    }

    private boolean handleStorageCommand(CommandSender sender, String label, String[] args) {
        if (args.length < 4 || !args[1].equalsIgnoreCase("migrate")) {
            sender.sendMessage(plugin.getLanguageManager().withPrefix(
                    "command.storage-migrate-usage",
                    Map.of("label", label)));
            return true;
        }

        String from = args[2].toUpperCase();
        String to = args[3].toUpperCase();

        if (!plugin.getStorageManager().isValidStorageType(from)
                || !plugin.getStorageManager().isValidStorageType(to)) {
            sender.sendMessage(plugin.getLanguageManager().withPrefix("storage.migration-invalid"));
            return true;
        }

        if (from.equalsIgnoreCase(to)) {
            sender.sendMessage(plugin.getLanguageManager().withPrefix("storage.migration-same-type"));
            return true;
        }

        sender.sendMessage(plugin.getLanguageManager().withPrefix(
                "storage.migration-started",
                Map.of(
                        "from", from,
                        "to", to)));

        int migrated = plugin.getStorageManager().migrateCount(from, to);

        if (migrated < 0) {
            sender.sendMessage(plugin.getLanguageManager().withPrefix(
                    "storage.migration-failed",
                    Map.of("error", "unknown")));
            return true;
        }

        if (migrated == 0) {
            sender.sendMessage(plugin.getLanguageManager()
                    .withPrefix("storage.migration-empty"));
            return true;
        }

        sender.sendMessage(plugin.getLanguageManager().withPrefix(
                "storage.migration-success",
                Map.of(
                        "from", from,
                        "to", to,
                        "amount", String.valueOf(migrated))));

        return true;
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(plugin.getLanguageManager().color("&8&m--------------------------------"));
        sender.sendMessage(plugin.getLanguageManager().get(
                "help.title",
                Map.of("label", label)));
        sender.sendMessage(plugin.getLanguageManager().get(
                "help.help",
                Map.of("label", label)));
        sender.sendMessage(plugin.getLanguageManager().get(
                "help.give",
                Map.of("label", label)));
        sender.sendMessage(plugin.getLanguageManager().get(
                "help.trust",
                Map.of("label", label)));
        sender.sendMessage(plugin.getLanguageManager().get(
                "help.untrust",
                Map.of("label", label)));
        sender.sendMessage(plugin.getLanguageManager().get(
                "help.trusted",
                Map.of("label", label)));
        sender.sendMessage(plugin.getLanguageManager().get(
                "help.storage-migrate",
                Map.of("label", label)));
        sender.sendMessage(plugin.getLanguageManager().get(
                "help.reload",
                Map.of("label", label)));
        sender.sendMessage(plugin.getLanguageManager().color("&8&m--------------------------------"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> result = new ArrayList<String>();

        if (args.length == 1) {
            List<String> subcommands = Arrays.asList("help", "give", "reload", "trust", "untrust", "trusted",
                    "storage");

            for (String subcommand : subcommands) {
                if (subcommand.toLowerCase().startsWith(args[0].toLowerCase())) {
                    result.add(subcommand);
                }
            }

            return result;
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("give")
                || args[0].equalsIgnoreCase("trust")
                || args[0].equalsIgnoreCase("untrust"))) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                    result.add(player.getName());
                }
            }

            return result;
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            return Arrays.asList("1", "2", "4", "8", "16", "32", "64");
        }

        return result;
    }

    private BeaconData getTargetBeacon(Player player) {
        Block block = player.getTargetBlock(null, 6);

        if (block.getType() != Material.BEACON) {
            return null;
        }

        return plugin.getBeaconManager().getBeacon(block.getLocation());
    }
}