package cl.dynasty.dynabeacon.effects.executor;

import cl.dynasty.dynabeacon.effects.BeaconEffect;
import cl.dynasty.dynabeacon.model.BeaconData;

public interface EffectExecutor {

    String getType();

    void tick(BeaconData beacon, BeaconEffect effect);
}