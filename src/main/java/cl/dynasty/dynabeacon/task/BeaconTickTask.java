package cl.dynasty.dynabeacon.task;

import cl.dynasty.dynabeacon.DynaBeaconPlugin;
import cl.dynasty.dynabeacon.effects.BeaconEffect;
import cl.dynasty.dynabeacon.model.BeaconData;

public class BeaconTickTask implements Runnable {

    private final DynaBeaconPlugin plugin;

    public BeaconTickTask(DynaBeaconPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        for (BeaconData beacon : plugin.getBeaconManager().getBeacons()) {
            for (String effectId : beacon.getActiveEffects()) {
                BeaconEffect effect = plugin.getEffectRegistry().getEffect(effectId);

                if (effect == null) {
                    continue;
                }

                plugin.getEffectExecutorRegistry().tick(beacon, effect);
            }
        }
    }
}