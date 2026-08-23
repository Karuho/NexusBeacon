package cl.dynasty.nexusbeacon.platform.legacy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** Productive Java-8 command subset backed by authoritative Legacy state. */
public final class LegacyNexusBeaconCommand implements CommandExecutor, TabCompleter {
    private static final String ADMIN_PERMISSION = "NexusBeacon.admin";
    private final LegacyApplicationGraph application;
    private final LegacyBeaconItemFactory items;
    private final LegacyTextFormatter text;
    private final LegacyCommandEnvironment environment;
    private final String prefix;

    public LegacyNexusBeaconCommand(LegacyApplicationGraph application, LegacyBeaconItemFactory items,
            LegacyTextFormatter text, LegacyCommandEnvironment environment, String prefix) {
        if (application == null || items == null || text == null || environment == null) {
            throw new NullPointerException("Legacy command dependency");
        }
        application.requireAvailable(LegacyApplicationCapability.STORAGE);
        application.requireAvailable(LegacyApplicationCapability.IDENTITY);
        this.application = application;
        this.items = items;
        this.text = text;
        this.environment = environment;
        this.prefix = prefix == null ? "" : prefix;
    }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || equals(args[0], "help")) {
            help(sender, label);
            return true;
        }
        if (equals(args[0], "give")) return give(sender, label, args);
        if (equals(args[0], "trust")) return trust(sender, label, args, true);
        if (equals(args[0], "untrust")) return trust(sender, label, args, false);
        if (equals(args[0], "trusted")) return trusted(sender);
        if (equals(args[0], "reload") || equals(args[0], "storage")) {
            if (!sender.hasPermission(ADMIN_PERMISSION)) return denied(sender);
            send(sender, "&cThis command branch is unavailable on Legacy and made no changes.");
            return true;
        }
        send(sender, "&cUnknown subcommand. Use &f/" + label + " help&c.");
        return true;
    }

    private boolean give(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission(ADMIN_PERMISSION)) return denied(sender);
        if (args.length < 2) {
            send(sender, "&cUsage: /" + label + " give <player> [amount]");
            return true;
        }
        Player target = environment.findOnlinePlayer(args[1]);
        if (target == null) {
            send(sender, "&cPlayer not found.");
            return true;
        }
        int amount = 1;
        if (args.length >= 3) {
            try { amount = Integer.parseInt(args[2]); }
            catch (NumberFormatException ignored) { amount = 1; }
        }
        if (amount < 1) amount = 1;
        if (amount > 64) amount = 64;
        ItemStack item = items.createNew(amount);
        target.getInventory().addItem(item);
        send(sender, "&aGave &f" + amount + " &aNexusBeacon item(s) to &f" + target.getName() + "&a.");
        send(target, "&aYou received a NexusBeacon.");
        return true;
    }

    private boolean trusted(CommandSender sender) {
        Player player = player(sender);
        if (player == null) return true;
        LegacyBeaconState beacon = readyTarget(player);
        if (beacon == null) return true;
        send(player, "&eTrusted players: &f" + beacon.getTrustedPlayers().size());
        return true;
    }

    private boolean trust(CommandSender sender, String label, String[] args, boolean add) {
        Player player = player(sender);
        if (player == null) return true;
        if (args.length < 2) {
            send(player, "&cUsage: /" + label + " " + (add ? "trust" : "untrust") + " <player>");
            return true;
        }
        LegacyBeaconState beacon = readyTarget(player);
        if (beacon == null) return true;
        UUID owner = beacon.getOwner();
        if (!(owner != null && owner.equals(player.getUniqueId())) && !player.hasPermission(ADMIN_PERMISSION)) {
            send(player, "&cYou cannot manage this NexusBeacon.");
            return true;
        }
        Player target = environment.findOnlinePlayer(args[1]);
        if (target == null) {
            send(player, "&cPlayer is not online.");
            return true;
        }
        if (add && owner != null && owner.equals(target.getUniqueId())) {
            send(player, "&cYou cannot add the owner as a trusted player.");
            return true;
        }
        Set<UUID> trusted = new TreeSet<UUID>(beacon.getTrustedPlayers());
        if (add) trusted.add(target.getUniqueId()); else trusted.remove(target.getUniqueId());
        try {
            if (!application.getState().update(beacon.withTrustedPlayers(trusted))) {
                send(player, "&cThe NexusBeacon changed before the command completed; no change was published.");
                return true;
            }
        } catch (LegacyStorageException exception) {
            send(player, "&cThe persistent update failed safely; no change was published.");
            return true;
        }
        send(player, add ? "&a" + target.getName() + " is now trusted."
                : "&cRemoved trust from &f" + target.getName() + "&c.");
        return true;
    }

    private LegacyBeaconState readyTarget(Player player) {
        if (!application.getState().getStatus().isReady()) {
            send(player, "&cNexusBeacon storage is not ready.");
            return null;
        }
        LegacyBeaconState beacon = environment.findTargetBeacon(player);
        if (beacon == null) send(player, "&cLook directly at a registered NexusBeacon.");
        return beacon;
    }

    private Player player(CommandSender sender) {
        if (sender instanceof Player) return (Player) sender;
        send(sender, "&cOnly players can use this command.");
        return null;
    }

    private boolean denied(CommandSender sender) {
        send(sender, "&cYou do not have permission to do that.");
        return true;
    }

    private void help(CommandSender sender, String label) {
        send(sender, "&8&m--------------------------------");
        send(sender, "&bNexusBeacon Legacy commands");
        send(sender, "&f/" + label + " help &7- Shows this help.");
        send(sender, "&f/" + label + " trusted &7- Counts trusted players on the targeted beacon.");
        send(sender, "&f/" + label + " trust <player> &7- Trusts an online player.");
        send(sender, "&f/" + label + " untrust <player> &7- Removes trust.");
        if (sender.hasPermission(ADMIN_PERMISSION)) {
            send(sender, "&f/" + label + " give <player> [amount] &7- Gives a marked NexusBeacon.");
        }
        send(sender, "&8&m--------------------------------");
    }

    @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> values = new ArrayList<String>(Arrays.asList("help", "trust", "untrust", "trusted"));
            if (sender.hasPermission(ADMIN_PERMISSION)) values.addAll(Arrays.asList("give", "reload", "storage"));
            return matching(values, args[0]);
        }
        if (args.length == 3 && equals(args[0], "give")) {
            return matching(Arrays.asList("1", "2", "4", "8", "16", "32", "64"), args[2]);
        }
        return Collections.emptyList();
    }

    private List<String> matching(List<String> values, String prefix) {
        List<String> result = new ArrayList<String>();
        String expected = prefix.toLowerCase(java.util.Locale.ROOT);
        for (String value : values) if (value.toLowerCase(java.util.Locale.ROOT).startsWith(expected)) result.add(value);
        return result;
    }

    private void send(CommandSender sender, String message) { sender.sendMessage(text.format(prefix + message)); }
    private static boolean equals(String left, String right) { return left.equalsIgnoreCase(right); }
}
