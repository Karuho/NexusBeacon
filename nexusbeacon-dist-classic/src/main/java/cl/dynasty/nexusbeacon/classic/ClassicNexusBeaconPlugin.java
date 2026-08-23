package cl.dynasty.nexusbeacon.classic;

import org.bukkit.plugin.java.JavaPlugin;
import cl.dynasty.nexusbeacon.platform.classic.ClassicPlatform;

public final class ClassicNexusBeaconPlugin extends JavaPlugin {
    private ClassicPlatform platform;
    @Override public void onEnable() {
        try {
            platform = ClassicPlatform.create(this, getServer().getBukkitVersion());
            saveDefaultConfig();
            getLogger().info("Classic foundation initialized for Minecraft " + platform.getCapabilities().getVersion()
                    + " using " + platform.getCapabilities().getIdentityBackend() + ".");
        } catch (RuntimeException unsupported) {
            getLogger().severe("Classic startup refused: " + unsupported.getMessage());
            getServer().getPluginManager().disablePlugin(this);
        }
    }
    @Override public void onDisable() { platform = null; }
    public ClassicPlatform getPlatform() { return platform; }
}
