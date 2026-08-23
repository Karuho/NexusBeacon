package cl.dynasty.nexusbeacon.platform.classic;

import org.bukkit.plugin.Plugin;
import cl.dynasty.nexusbeacon.platform.MinecraftVersion;
import cl.dynasty.nexusbeacon.platform.api.ItemIdentityService;
import cl.dynasty.nexusbeacon.platform.api.MaterialResolver;
import cl.dynasty.nexusbeacon.platform.api.PlatformServices;
import cl.dynasty.nexusbeacon.platform.api.PotionEffectResolver;

public final class ClassicPlatform {
    private final ClassicCapabilities capabilities;
    private final ItemIdentityService itemIdentity;
    private final MaterialResolver materials;
    private final PotionEffectResolver potions;
    private final ClassicParticleResolver particles;
    private final PlatformServices services;

    private ClassicPlatform(Plugin plugin, ClassicCapabilities capabilities) {
        this.capabilities = capabilities;
        this.itemIdentity = new ClassicItemIdentityService(plugin, capabilities);
        this.materials = new ClassicMaterialResolver(); this.potions = new ClassicPotionEffectResolver(); this.particles = new ClassicParticleResolver();
        ClassicSchedulerService scheduler = new ClassicSchedulerService(plugin);
        this.services = new PlatformServices(scheduler, new ClassicTeleporterService(scheduler));
    }
    public static ClassicPlatform create(Plugin plugin, String bukkitVersion) {
        return new ClassicPlatform(plugin, ClassicCapabilities.forVersion(MinecraftVersion.parse(bukkitVersion)));
    }
    public ClassicCapabilities getCapabilities() { return capabilities; }
    public ItemIdentityService getItemIdentity() { return itemIdentity; }
    public MaterialResolver getMaterials() { return materials; }
    public PotionEffectResolver getPotions() { return potions; }
    public ClassicParticleResolver getParticles() { return particles; }
    public PlatformServices getServices() { return services; }
}
