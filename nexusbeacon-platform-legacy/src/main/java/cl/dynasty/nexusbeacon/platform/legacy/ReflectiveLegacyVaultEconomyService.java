package cl.dynasty.nexusbeacon.platform.legacy;

import java.lang.reflect.Method;

import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

/** Resolves Vault only at action time, so absence can never break Legacy class loading. */
public final class ReflectiveLegacyVaultEconomyService implements LegacyEconomyService {
    private final Plugin plugin;
    private String diagnostic = "Vault is unavailable";

    public ReflectiveLegacyVaultEconomyService(Plugin plugin) {
        if (plugin == null) throw new NullPointerException("plugin");
        this.plugin = plugin;
    }

    @Override public boolean isAvailable() { return provider() != null; }
    @Override public String getDiagnostic() { return diagnostic; }

    @Override public boolean has(Player player, double amount) {
        Object provider = provider();
        return provider != null && Boolean.TRUE.equals(invoke(provider, "has",
                new Class<?>[] { org.bukkit.OfflinePlayer.class, double.class }, player, Double.valueOf(amount)));
    }

    @Override public boolean withdraw(Player player, double amount) {
        Object provider = provider();
        return provider != null && successful(invoke(provider, "withdrawPlayer",
                new Class<?>[] { org.bukkit.OfflinePlayer.class, double.class }, player, Double.valueOf(amount)));
    }

    @Override public boolean deposit(Player player, double amount) {
        Object provider = provider();
        return provider != null && successful(invoke(provider, "depositPlayer",
                new Class<?>[] { org.bukkit.OfflinePlayer.class, double.class }, player, Double.valueOf(amount)));
    }

    private Object provider() {
        try {
            Server server = plugin.getServer();
            Plugin vault = server.getPluginManager().getPlugin("Vault");
            if (vault == null || !vault.isEnabled()) {
                diagnostic = "Vault is unavailable";
                return null;
            }
            Class<?> economy = Class.forName("net.milkbowl.vault.economy.Economy", false,
                    vault.getClass().getClassLoader());
            RegisteredServiceProvider<?> registration = server.getServicesManager().getRegistration(economy);
            if (registration == null || registration.getProvider() == null) {
                diagnostic = "Vault has no economy provider";
                return null;
            }
            diagnostic = "available";
            return registration.getProvider();
        } catch (Throwable failure) {
            diagnostic = "Vault economy resolution failed: " + failure.getClass().getSimpleName();
            return null;
        }
    }

    private Object invoke(Object target, String name, Class<?>[] types, Object... arguments) {
        try {
            Method method = target.getClass().getMethod(name, types);
            return method.invoke(target, arguments);
        } catch (Throwable failure) {
            diagnostic = "Vault operation failed: " + failure.getClass().getSimpleName();
            return null;
        }
    }

    private boolean successful(Object response) {
        if (response == null) return false;
        try {
            return Boolean.TRUE.equals(response.getClass().getMethod("transactionSuccess").invoke(response));
        } catch (Throwable failure) {
            diagnostic = "Vault response failed: " + failure.getClass().getSimpleName();
            return false;
        }
    }
}
