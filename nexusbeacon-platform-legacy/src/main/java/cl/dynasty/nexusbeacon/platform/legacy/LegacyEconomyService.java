package cl.dynasty.nexusbeacon.platform.legacy;

import org.bukkit.entity.Player;

/** Optional money boundary; production domain code has no Vault type descriptors. */
public interface LegacyEconomyService {
    boolean isAvailable();
    boolean has(Player player, double amount);
    boolean withdraw(Player player, double amount);
    boolean deposit(Player player, double amount);
    String getDiagnostic();
}
