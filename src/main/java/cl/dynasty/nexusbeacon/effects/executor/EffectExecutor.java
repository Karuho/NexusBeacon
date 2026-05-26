package cl.dynasty.nexusbeacon.effects.executor;

import cl.dynasty.nexusbeacon.effects.BeaconEffect;
import cl.dynasty.nexusbeacon.model.BeaconData;

public interface EffectExecutor {

    String getType();

    void tick(BeaconData beacon, BeaconEffect effect);
}