package cl.dynasty.nexusbeacon.platform.classic;
import org.bukkit.entity.Player;
interface ClassicEconomyService {boolean isAvailable();boolean has(Player player,double amount);boolean withdraw(Player player,double amount);boolean deposit(Player player,double amount);String diagnostic();}
