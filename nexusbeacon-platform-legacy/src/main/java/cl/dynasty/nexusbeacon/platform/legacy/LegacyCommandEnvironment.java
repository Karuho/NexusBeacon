package cl.dynasty.nexusbeacon.platform.legacy;

import org.bukkit.entity.Player;

/** Bukkit lookups isolated for deterministic command tests and Legacy runtime use. */
public interface LegacyCommandEnvironment {
    Player findOnlinePlayer(String name);
    LegacyBeaconState findTargetBeacon(Player player);
}
