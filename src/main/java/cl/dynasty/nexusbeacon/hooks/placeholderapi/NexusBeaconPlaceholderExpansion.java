package cl.dynasty.nexusbeacon.hooks.placeholderapi;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import cl.dynasty.nexusbeacon.NexusBeaconPlugin;
import cl.dynasty.nexusbeacon.model.BeaconData;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;

public final class NexusBeaconPlaceholderExpansion extends PlaceholderExpansion {

    private final NexusBeaconPlugin plugin;

    public NexusBeaconPlaceholderExpansion(NexusBeaconPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "nexusbeacon";
    }

    @Override
    public String getAuthor() {
        return "Karuho";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer offlinePlayer, String params) {
        if (offlinePlayer == null) {
            return "";
        }

        Player player = offlinePlayer.getPlayer();

        if (params.equalsIgnoreCase("plugin_name")) {
            return plugin.getName();
        }

        if (params.equalsIgnoreCase("plugin_version")) {
            return plugin.getDescription().getVersion();
        }

        if (params.equalsIgnoreCase("player")) {
            return offlinePlayer.getName() != null ? offlinePlayer.getName() : "";
        }

        if (params.equalsIgnoreCase("player_uuid")) {
            return offlinePlayer.getUniqueId().toString();
        }

        if (player == null) {
            return "";
        }

        BeaconData beacon = plugin.getBeaconManager().getNearestBeacon(player.getLocation());

        if (beacon == null) {
            return "";
        }

        return switch (params.toLowerCase()) {
            case "beacon_id" -> beacon.getId();
            case "beacon_unique_id" -> beacon.getUniqueId();
            case "beacon_owner" -> Bukkit.getOfflinePlayer(beacon.getOwner()).getName();
            case "active_effects" -> String.valueOf(beacon.getActiveEffects().size());
            case "beacon_world" -> beacon.getLocation().getWorld().getName();
            case "beacon_x" -> String.valueOf(beacon.getLocation().getBlockX());
            case "beacon_y" -> String.valueOf(beacon.getLocation().getBlockY());
            case "beacon_z" -> String.valueOf(beacon.getLocation().getBlockZ());
            case "beacon_location" -> String.format(
                    "%s, %s, %s",
                    beacon.getLocation().getBlockX(),
                    beacon.getLocation().getBlockY(),
                    beacon.getLocation().getBlockZ()
            );
            case "beacon_range" -> String.valueOf(beacon.getRange());
            case "beacon_level" -> String.valueOf(beacon.getLevel());
            case "trusted_players" -> String.valueOf(beacon.getTrustedPlayers().size());
            case "protect_base_blocks" -> beacon.isProtectBaseBlocks() ? "Sí" : "No";
            default -> null;
        };
    }
}