package cl.dynasty.dynabeacon.effects.executor;

import cl.dynasty.dynabeacon.effects.BeaconEffect;
import cl.dynasty.dynabeacon.model.BeaconData;

public class FurnaceBoostExecutor implements EffectExecutor {

    @Override
    public String getType() {
        return "FURNACE_BOOST";
    }

    @Override
    public void tick(BeaconData beacon, BeaconEffect effect) {
        // Próximo bloque: acelerar hornos dentro del rango.
    }
}