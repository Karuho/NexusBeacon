package cl.dynasty.nexusbeacon.effects.executor;

import cl.dynasty.nexusbeacon.effects.BeaconEffect;
import cl.dynasty.nexusbeacon.model.BeaconData;

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