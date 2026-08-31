package cl.dynasty.nexusbeacon.platform.classic;
import static org.junit.jupiter.api.Assertions.*;import java.lang.reflect.Proxy;import org.bukkit.Server;import org.bukkit.plugin.Plugin;import org.bukkit.plugin.PluginManager;import org.junit.jupiter.api.Test;
class ClassicVaultEconomyServiceTest {
 @Test void vaultAbsentIsOptionalAndUnavailable(){ClassicVaultEconomyService service=new ClassicVaultEconomyService(host(null));assertFalse(service.isAvailable());assertEquals("Vault unavailable",service.diagnostic());}
 @Test void vaultWithoutLoadableEconomyProviderFailsClosed(){Plugin vault=pluginProxy(null);ClassicVaultEconomyService service=new ClassicVaultEconomyService(host(vault));assertFalse(service.isAvailable());assertTrue(service.diagnostic().startsWith("Vault resolution failed"));}
 private static Plugin host(final Plugin vault){final PluginManager manager=(PluginManager)Proxy.newProxyInstance(PluginManager.class.getClassLoader(),new Class<?>[]{PluginManager.class},(p,m,a)->m.getName().equals("getPlugin")?vault:defaultValue(m.getReturnType()));final Server server=(Server)Proxy.newProxyInstance(Server.class.getClassLoader(),new Class<?>[]{Server.class},(p,m,a)->m.getName().equals("getPluginManager")?manager:defaultValue(m.getReturnType()));return pluginProxy(server);}
 private static Plugin pluginProxy(final Server server){return (Plugin)Proxy.newProxyInstance(Plugin.class.getClassLoader(),new Class<?>[]{Plugin.class},(p,m,a)->m.getName().equals("getServer")?server:m.getName().equals("isEnabled")?true:defaultValue(m.getReturnType()));}
 private static Object defaultValue(Class<?> type){if(type==boolean.class)return false;if(type==int.class)return 0;if(type==long.class)return 0L;if(type==double.class)return 0D;if(type==float.class)return 0F;return null;}
}
