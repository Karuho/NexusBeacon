package cl.dynasty.dynabeacon.effects.executor;

import cl.dynasty.dynabeacon.DynaBeaconPlugin;
import cl.dynasty.dynabeacon.effects.BeaconEffect;
import cl.dynasty.dynabeacon.model.BeaconData;

import java.util.HashMap;
import java.util.Map;

public class EffectExecutorRegistry {

    private final DynaBeaconPlugin plugin;
    private final Map<String, EffectExecutor> executors = new HashMap<String, EffectExecutor>();

    public EffectExecutorRegistry(DynaBeaconPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        executors.clear();

        register(new PotionEffectExecutor());
        register(new BlockProcessBoostExecutor(plugin));
        register(new CropBoostExecutor(plugin));
        register(new SpawnerBoostExecutor(plugin));
        register(new BurnerExecutor(plugin));
        register(new DamageFieldExecutor(plugin));
        register(new GravityWellExecutor(plugin));

        plugin.getLogger().info("EffectExecutors cargados: " + executors.size());
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