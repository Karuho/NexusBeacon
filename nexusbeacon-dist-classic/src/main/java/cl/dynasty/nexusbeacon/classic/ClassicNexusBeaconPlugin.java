package cl.dynasty.nexusbeacon.classic;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import cl.dynasty.nexusbeacon.platform.api.ItemIdentity;
import cl.dynasty.nexusbeacon.platform.classic.ClassicApplication;
import cl.dynasty.nexusbeacon.platform.classic.ClassicPlatform;

public final class ClassicNexusBeaconPlugin extends JavaPlugin {
    private ClassicPlatform platform;
    private ClassicApplication application;
    @Override public void onEnable() {
        try {
            platform = ClassicPlatform.create(this, getServer().getBukkitVersion());
            verifyIdentityContract();
            saveResources();
            application = new ClassicApplication(this, platform);
            FileConfiguration main = load("config.yml");
            String language = main.getString("language", "en_us");
            application.start(load("beacon.yml"), load("effects.yml"), load("languages/" + language + ".yml"));
            getLogger().info("Classic foundation initialized for Minecraft " + platform.getCapabilities().getVersion()
                    + " using " + platform.getCapabilities().getIdentityBackend() + ".");
        } catch (RuntimeException unsupported) {
            getLogger().severe("Classic startup refused: " + unsupported.getMessage());
            getServer().getPluginManager().disablePlugin(this);
        }
    }
    @Override public void onDisable() { if (application != null) application.stop(); application = null; platform = null; }
    public ClassicPlatform getPlatform() { return platform; }
    private void saveResources() { String[] files={"config.yml","beacon.yml","effects.yml","gui.yml","storage.yml","languages/en_us.yml","languages/es_cl.yml"};for(String file:files)if(!new File(getDataFolder(),file).isFile())saveResource(file,false); }
    private FileConfiguration load(String path) { return YamlConfiguration.loadConfiguration(new File(getDataFolder(), path)); }
    private void verifyIdentityContract() {
        ItemStack ordinary = new ItemStack(Material.BEACON);
        ItemMeta spoofMeta = ordinary.getItemMeta();
        spoofMeta.setDisplayName("NexusBeacon");
        spoofMeta.setLore(java.util.Collections.singletonList("NexusBeacon"));
        ordinary.setItemMeta(spoofMeta);
        if (platform.getItemIdentity().identify(ordinary).isRecognized()) throw new IllegalStateException("Classic identity accepted display/lore spoof");
        ItemStack marked = platform.getItemIdentity().mark(new ItemStack(Material.BEACON), ItemIdentity.NEXUS_BEACON);
        if (!platform.getItemIdentity().identify(marked).isRecognized()
                || !platform.getItemIdentity().identify(marked.clone()).isRecognized()
                || !platform.getItemIdentity().identify(ItemStack.deserialize(marked.serialize())).isRecognized()) {
            throw new IllegalStateException("Classic identity clone/serialization self-check failed");
        }
    }
}
