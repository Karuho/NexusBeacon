package cl.dynasty.dynabeacon.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import cl.dynasty.dynabeacon.DynaBeaconPlugin;
import cl.dynasty.dynabeacon.model.BeaconData;
import cl.dynasty.dynabeacon.storage.BeaconStorageProvider;
import cl.dynasty.dynabeacon.storage.MySqlBeaconStorageProvider;
import cl.dynasty.dynabeacon.storage.SqliteBeaconStorageProvider;
import cl.dynasty.dynabeacon.storage.YamlBeaconStorageProvider;
import cl.dynasty.dynabeacon.util.ColorUtil;

public class DynaBeaconCommand implements CommandExecutor, TabCompleter {

    private final DynaBeaconPlugin plugin;

    public DynaBeaconCommand(DynaBeaconPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender, label);
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("dynabeacon.admin")) {
                sender.sendMessage(ColorUtil.color("&b[DynaBeacon]&r &cNo tienes permiso."));
                return true;
            }

            plugin.reloadAll();
            sender.sendMessage(ColorUtil.color("&b[DynaBeacon]&r &aConfiguración recargada."));
            return true;
        }

        if (args[0].equalsIgnoreCase("give")) {
            if (!sender.hasPermission("dynabeacon.admin")) {
                sender.sendMessage(ColorUtil.color("&b[DynaBeacon]&r &cNo tienes permiso."));
                return true;
            }

            if (args.length < 2) {
                sender.sendMessage(ColorUtil.color("&b[DynaBeacon]&r &cUso: /" + label + " give <jugador> [cantidad]"));
                return true;
            }

            Player target = Bukkit.getPlayer(args[1]);

            if (target == null) {
                sender.sendMessage(ColorUtil.color("&b[DynaBeacon]&r &cJugador no encontrado."));
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

            sender.sendMessage(ColorUtil.color(
                    "&b[DynaBeacon]&r &aEntregaste &f" + amount + " &aDynaBeacon a &f" + target.getName() + "&a."));
            target.sendMessage(ColorUtil.color("&b[DynaBeacon]&r &aRecibiste un DynaBeacon."));
            return true;
        }

        if (args[0].equalsIgnoreCase("trust")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ColorUtil.color("&b[DynaBeacon]&r &cSolo jugadores."));
                return true;
            }

            Player player = (Player) sender;

            if (args.length < 2) {
                player.sendMessage(ColorUtil.color("&b[DynaBeacon]&r &cUso: /" + label + " trust <jugador>"));
                return true;
            }

            BeaconData beacon = getTargetBeacon(player);

            if (beacon == null) {
                player.sendMessage(ColorUtil.color("&b[DynaBeacon]&r &cDebes mirar un DynaBeacon."));
                return true;
            }

            if (!beacon.canManage(player.getUniqueId()) && !player.hasPermission("dynabeacon.admin")) {
                player.sendMessage(ColorUtil.color("&b[DynaBeacon]&r &cNo puedes administrar este DynaBeacon."));
                return true;
            }

            Player target = Bukkit.getPlayer(args[1]);

            if (target == null) {
                player.sendMessage(ColorUtil.color("&b[DynaBeacon]&r &cJugador no encontrado o no está online."));
                return true;
            }

            beacon.addTrusted(target.getUniqueId());
            plugin.getStorageManager().saveBeacon(beacon);

            player.sendMessage(ColorUtil.color("&b[DynaBeacon]&r &aAhora &f" + target.getName() + " &aes confiable."));
            return true;
        }

        if (args[0].equalsIgnoreCase("untrust")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ColorUtil.color("&b[DynaBeacon]&r &cSolo jugadores."));
                return true;
            }

            Player player = (Player) sender;

            if (args.length < 2) {
                player.sendMessage(ColorUtil.color("&b[DynaBeacon]&r &cUso: /" + label + " untrust <jugador>"));
                return true;
            }

            BeaconData beacon = getTargetBeacon(player);

            if (beacon == null) {
                player.sendMessage(ColorUtil.color("&b[DynaBeacon]&r &cDebes mirar un DynaBeacon."));
                return true;
            }

            if (!beacon.canManage(player.getUniqueId()) && !player.hasPermission("dynabeacon.admin")) {
                player.sendMessage(ColorUtil.color("&b[DynaBeacon]&r &cNo puedes administrar este DynaBeacon."));
                return true;
            }

            Player target = Bukkit.getPlayer(args[1]);

            if (target == null) {
                player.sendMessage(ColorUtil.color("&b[DynaBeacon]&r &cJugador no encontrado o no está online."));
                return true;
            }

            beacon.removeTrusted(target.getUniqueId());
            plugin.getStorageManager().saveBeacon(beacon);

            player.sendMessage(ColorUtil.color("&b[DynaBeacon]&r &cQuitaste trust a &f" + target.getName() + "&c."));
            return true;
        }

        if (args[0].equalsIgnoreCase("trusted")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ColorUtil.color("&b[DynaBeacon]&r &cSolo jugadores."));
                return true;
            }

            Player player = (Player) sender;
            BeaconData beacon = getTargetBeacon(player);

            if (beacon == null) {
                player.sendMessage(ColorUtil.color("&b[DynaBeacon]&r &cDebes mirar un DynaBeacon."));
                return true;
            }

            player.sendMessage(
                    ColorUtil.color("&b[DynaBeacon]&r &eJugadores confiables: &f" + beacon.getTrustedPlayers().size()));
            return true;
        }

        if (args[0].equalsIgnoreCase("storage")) {
            if (!sender.hasPermission("dynabeacon.admin")) {
                sender.sendMessage(ColorUtil.color("&b[DynaBeacon]&r &cNo tienes permiso."));
                return true;
            }

            return handleStorageCommand(sender, label, args);
        }

        sender.sendMessage(ColorUtil.color("&b[DynaBeacon]&r &cSubcomando desconocido. Usa &f/" + label + " help&c."));
        return true;
    }

    private boolean handleStorageCommand(CommandSender sender, String label, String[] args) {
    if (args.length < 4 || !args[1].equalsIgnoreCase("migrate")) {
        sender.sendMessage(ColorUtil.color("&b[DynaBeacon]&r &cUso: /" + label
                + " storage migrate <YAML|MYSQL|SQLITE> <YAML|MYSQL|SQLITE>"));
        return true;
    }

    String from = args[2].toUpperCase();
    String to = args[3].toUpperCase();

    BeaconStorageProvider source = createStorageProvider(from);
    BeaconStorageProvider target = createStorageProvider(to);

    List<BeaconData> beacons = source.loadBeacons();

    for (BeaconData beacon : beacons) {
        target.saveBeacon(beacon);
    }

    source.close();
    target.close();

    sender.sendMessage(ColorUtil.color("&b[DynaBeacon]&r &aMigración completada: &f"
            + from + " &7-> &f" + to + "&7. Beacons: &f" + beacons.size()));

    return true;
}

private BeaconStorageProvider createStorageProvider(String type) {
    if (type.equalsIgnoreCase("MYSQL")) {
        return new MySqlBeaconStorageProvider(plugin);
    }

    if (type.equalsIgnoreCase("SQLITE")) {
        return new SqliteBeaconStorageProvider(plugin);
    }

    return new YamlBeaconStorageProvider(plugin);
}

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(ColorUtil.color("&8&m--------------------------------"));
        sender.sendMessage(ColorUtil.color("&b&lDynaBeacon &7- Comandos"));
        sender.sendMessage(ColorUtil.color("&f/" + label + " help &7- Muestra esta ayuda."));
        sender.sendMessage(ColorUtil.color("&f/" + label + " give <jugador> [cantidad] &7- Entrega DynaBeacon."));
        sender.sendMessage(
                ColorUtil.color("&f/" + label + " trust <jugador> &7- Confía un jugador en el DynaBeacon que miras."));
        sender.sendMessage(ColorUtil.color("&f/" + label + " untrust <jugador> &7- Quita trust."));
        sender.sendMessage(ColorUtil.color("&f/" + label + " trusted &7- Lista jugadores confiables."));
        sender.sendMessage(ColorUtil.color("&f/" + label + " storage migrate <origen> <destino> &7- Migra storage."));
        sender.sendMessage(ColorUtil.color("&f/" + label + " reload &7- Recarga configuración."));
        sender.sendMessage(ColorUtil.color("&8&m--------------------------------"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> result = new ArrayList<String>();

        if (args.length == 1) {
            List<String> subcommands = Arrays.asList("help", "give", "reload", "trust", "untrust", "trusted", "storage");

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

        if (block == null || block.getType() != Material.BEACON) {
            return null;
        }

        return plugin.getBeaconManager().getBeacon(block.getLocation());
    }
}