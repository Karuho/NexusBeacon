package cl.dynasty.nexusbeacon.effects.executor;

import cl.dynasty.nexusbeacon.NexusBeaconPlugin;
import cl.dynasty.nexusbeacon.effects.BeaconEffect;
import cl.dynasty.nexusbeacon.model.BeaconData;

import java.util.HashMap;
import java.util.Map;

public class EffectExecutorRegistry {

    private final NexusBeaconPlugin plugin;
    private final Map<String, EffectExecutor> executors = new HashMap<String, EffectExecutor>();

    public EffectExecutorRegistry(NexusBeaconPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        executors.clear();

        register(new PotionEffectExecutor());
        register(new CropBoostExecutor(plugin));
        register(new SpawnerBoostExecutor(plugin));
        register(new BurnerExecutor(plugin));
        register(new DamageFieldExecutor(plugin));
        register(new GravityWellExecutor(plugin));
        register(new BlockProcessBoostExecutor(plugin));

        plugin.getLogger().info("EffectExecutors loaded: " + executors.size());
    }

    public void register(EffectExecutor executor) {
        executors.put(executor.getType().toUpperCase(), executor);
    }

    public void tick(BeaconData beacon, BeaconEffect effect) {
        EffectExecutor executor = executors.get(effect.getType().toUpperCase());

        if (executor == null) {
            return;
        }

        executor.tick(beacon, effect);
    }
}