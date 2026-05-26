package cl.dynasty.nexusbeacon.effects.executor;

import cl.dynasty.nexusbeacon.effects.BeaconEffect;
import cl.dynasty.nexusbeacon.model.BeaconData;

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