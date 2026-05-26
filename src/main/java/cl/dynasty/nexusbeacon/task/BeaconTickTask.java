package cl.dynasty.nexusbeacon.task;

import cl.dynasty.nexusbeacon.NexusBeaconPlugin;
import cl.dynasty.nexusbeacon.effects.BeaconEffect;
import cl.dynasty.nexusbeacon.model.BeaconData;

public class BeaconTickTask implements Runnable {

    private final NexusBeaconPlugin plugin;

    public BeaconTickTask(NexusBeaconPlugin plugin) {
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