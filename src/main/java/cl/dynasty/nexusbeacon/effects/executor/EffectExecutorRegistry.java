package cl.dynasty.nexusbeacon.effects.executor;

import cl.dynasty.nexusbeacon.NexusBeaconPlugin;
import cl.dynasty.nexusbeacon.effects.BeaconEffect;
import cl.dynasty.nexusbeacon.model.BeaconData;
import cl.dynasty.nexusbeacon.util.DebugLogger;

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
        register(new IgnitionExecutor(plugin));
        register(new DamageFieldExecutor(plugin));
        register(new GravityPulseExecutor(plugin));
        register(new BlockProcessBoostExecutor(plugin));

        plugin.getLogger().info("EffectExecutors loaded: " + executors.size());
    }

    public void register(EffectExecutor executor) {
        executors.put(executor.getType().toUpperCase(), executor);
    }

    public void tick(BeaconData beacon, BeaconEffect effect) {
        EffectExecutor executor = executors.get(effect.getType().toUpperCase());

        if (executor == null) {
            DebugLogger.log(plugin, "missing-executor:" + effect.getType(),
                    "No executor found for type=" + effect.getType()
                            + " effect=" + effect.getId());
            return;
        }

        executor.tick(beacon, effect);
    }
}