package cl.dynasty.dynabeacon.effects.executor;

import cl.dynasty.dynabeacon.effects.BeaconEffect;
import cl.dynasty.dynabeacon.model.BeaconData;

public class PotionEffectExecutor implements EffectExecutor {

    @Override
    public String getType() {
        return "POTION";
    }

    @Override
    public void tick(BeaconData beacon, BeaconEffect effect) {
        effect.tick(beacon);
    }
}